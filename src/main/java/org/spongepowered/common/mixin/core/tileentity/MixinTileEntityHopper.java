/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.core.tileentity;

import static org.spongepowered.api.data.DataQuery.of;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.api.block.tileentity.carrier.Hopper;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.manipulator.mutable.tileentity.CooldownData;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.entity.PlayerTracker;
import org.spongepowered.common.interfaces.IMixinChunk;
import org.spongepowered.common.interfaces.IMixinInventory;
import org.spongepowered.common.interfaces.entity.IMixinEntity;
import org.spongepowered.common.item.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.inventory.lens.Lens;
import org.spongepowered.common.item.inventory.lens.comp.GridInventoryLens;
import org.spongepowered.common.item.inventory.lens.impl.MinecraftLens;
import org.spongepowered.common.item.inventory.lens.impl.ReusableLens;
import org.spongepowered.common.item.inventory.lens.impl.collections.SlotCollection;
import org.spongepowered.common.item.inventory.lens.impl.comp.GridInventoryLensImpl;
import org.spongepowered.common.item.inventory.util.InventoryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NonnullByDefault
@Mixin(TileEntityHopper.class)
public abstract class MixinTileEntityHopper extends MixinTileEntityLockableLoot implements Hopper, IMixinInventory {

    @Shadow public int transferCooldown;
    @Shadow private static ItemStack insertStack(IInventory source, IInventory destination, ItemStack stack, int index, EnumFacing direction) {
        throw new AbstractMethodError("Shadow");
    }

    public List<SlotTransaction> capturedTransactions = new ArrayList<>();

    @Override
    public List<SlotTransaction> getCapturedTransactions() {
        return this.capturedTransactions;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "<init>", at = @At("RETURN"))
    public void onConstructed(CallbackInfo ci) {
        ReusableLens<? extends Lens<IInventory, ItemStack>> reusableLens = MinecraftLens.getLens(GridInventoryLens.class,
                ((InventoryAdapter) this),
                s -> new GridInventoryLensImpl(0, 5, 1, 5, s),
                () -> new SlotCollection.Builder().add(5).build());
        this.slots = reusableLens.getSlots();
        this.lens = reusableLens.getLens();
    }

    @Inject(method = "putDropInInventoryAllSlots", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/EntityItem;getItem()Lnet/minecraft/item/ItemStack;"))
    private static void onPutDrop(IInventory inventory, IInventory hopper, EntityItem entityItem, CallbackInfoReturnable<Boolean> callbackInfo) {
        ((IMixinEntity) entityItem).getCreatorUser().ifPresent(owner -> {
            if (inventory instanceof TileEntity) {
                TileEntity te = (TileEntity) inventory;
                BlockPos pos = te.getPos();
                IMixinChunk spongeChunk = (IMixinChunk) te.getWorld().getChunkFromBlockCoords(pos);
                spongeChunk.addTrackedBlockPosition(te.getBlockType(), pos, owner, PlayerTracker.Type.NOTIFIER);
            }
        });
    }

    @Override
    public DataContainer toContainer() {
        DataContainer container = super.toContainer();
        return container.set(of("TransferCooldown"), this.transferCooldown);
    }

    @Override
    public void supplyVanillaManipulators(List<DataManipulator<?, ?>> manipulators) {
        super.supplyVanillaManipulators(manipulators);
        Optional<CooldownData> cooldownData = get(CooldownData.class);
        if (cooldownData.isPresent()) {
            manipulators.add(cooldownData.get());
        }
    }

    @Inject(method = "transferItemsOut", locals = LocalCapture.CAPTURE_FAILEXCEPTION,
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;isEmpty()Z", ordinal = 1))
    private void afterPutStackInSlots(CallbackInfoReturnable<Boolean> cir, IInventory iInventory, EnumFacing enumFacing, int i, ItemStack itemStack, ItemStack itemStack1) {
        // after putStackInInventoryAllSlots if the transfer worked
        itemStack1 = InventoryUtil.handleTransferPost(((TileEntityHopper)(Object) this), i, itemStack, iInventory, itemStack1);
    }

    @Redirect(method = "putStackInInventoryAllSlots", at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/TileEntityHopper;insertStack(Lnet/minecraft/inventory/IInventory;Lnet/minecraft/inventory/IInventory;Lnet/minecraft/item/ItemStack;ILnet/minecraft/util/EnumFacing;)Lnet/minecraft/item/ItemStack;"))
    private static ItemStack onInsertStack(IInventory source, IInventory destination, ItemStack stack, int index, EnumFacing direction) {
        // capture Transaction
        if (!(source instanceof IMixinInventory && destination instanceof InventoryAdapter)) {
            return insertStack(source, destination, stack, index, direction);
        }

        return InventoryUtil
                .captureInsertRemote(source, ((InventoryAdapter) destination), index, () -> insertStack(source, destination, stack, index, direction));
    }

    @Inject(method = "transferItemsOut", cancellable = true, locals = LocalCapture.CAPTURE_FAILEXCEPTION, at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockHopper;getFacing(I)Lnet/minecraft/util/EnumFacing;"))
    private void onTransferItemsOut(CallbackInfoReturnable<Boolean> cir, IInventory iInventory) {
        if (InventoryUtil.handleTransferPre(((TileEntityHopper) ((Object) this)), iInventory).isCancelled()) {
            cir.setReturnValue(false);
        }
    }

}
