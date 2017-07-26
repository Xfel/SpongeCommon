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
package org.spongepowered.common.mixin.core.block;

import net.minecraft.block.BlockDropper;
import net.minecraft.block.BlockSourceImpl;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.item.inventory.util.InventoryUtil;

@Mixin(BlockDropper.class)
public abstract class MixinBlockDropper {

    /*
    TODO this is not working WHY?
    @Inject(method = "dispense", locals = LocalCapture.CAPTURE_FAILEXCEPTION,
            at = @At(value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/tileentity/TileEntityDispenser;setInventorySlotContents(ILnet/minecraft/item/ItemStack;)V"))
    private void afterDispense(World worldIn, BlockPos pos, CallbackInfo callbackInfo,
            BlockSourceImpl blocksourceimpl, TileEntityDispenser tileentitydispenser, int i, ItemStack itemstack,
            EnumFacing enumfacing, BlockPos blockpos, IInventory iinventory, ItemStack itemstack1) {
        // after setInventorySlotContents
        EventUtil.handleTransferPost(tileentitydispenser, i, itemstack, iinventory, itemstack);
    }
    */
    //TODO injecting after instead?
      @Inject(method = "dispense", cancellable = true, locals = LocalCapture.CAPTURE_FAILEXCEPTION,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/tileentity/TileEntityDispenser;setInventorySlotContents(ILnet/minecraft/item/ItemStack;)V"))
    private void afterDispense(World worldIn, BlockPos pos, CallbackInfo callbackInfo,
            BlockSourceImpl blocksourceimpl, TileEntityDispenser tileentitydispenser, int i, ItemStack itemstack,
            EnumFacing enumfacing, BlockPos blockpos, IInventory iinventory, ItemStack itemstack1) {
        // after setInventorySlotContents
        tileentitydispenser.setInventorySlotContents(i, itemstack1);
        InventoryUtil.handleTransferPost(tileentitydispenser, i, itemstack, iinventory, itemstack);
        callbackInfo.cancel();
    }

    @Inject(method = "dispense", cancellable = true, locals = LocalCapture.CAPTURE_FAILEXCEPTION,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/tileentity/TileEntityHopper;putStackInInventoryAllSlots(Lnet/minecraft/inventory/IInventory;Lnet/minecraft/inventory/IInventory;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/EnumFacing;)Lnet/minecraft/item/ItemStack;"))
    private void onDispense(World world, BlockPos pos, CallbackInfo ci,
            BlockSourceImpl blocksourceimpl, TileEntityDispenser tileentitydispenser, int i, ItemStack itemstack,
            EnumFacing enumfacing, BlockPos blockpos, IInventory iinventory) {
        // Before putStackInInventoryAllSlots
        if (InventoryUtil.handleTransferPre(tileentitydispenser, iinventory).isCancelled()) {
            ci.cancel();
        }
    }
}
