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
package org.spongepowered.common.item.inventory.util;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityLockableLoot;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.item.inventory.type.OrderedInventory;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.interfaces.IMixinInventory;
import org.spongepowered.common.item.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.inventory.adapter.impl.MinecraftInventoryAdapter;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class EventUtil {
    private EventUtil() {}

    public static ChangeInventoryEvent.Transfer.Pre handleTransferPre(TileEntity source, Object destination) {
        Cause.Builder builder = Cause.source(source);
        ChangeInventoryEvent.Transfer.Pre event = SpongeEventFactory.createChangeInventoryEventTransferPre(builder.build(), ((Inventory) source), ((Inventory) destination));
        SpongeImpl.postEvent(event);
        return event;
    }

    public static ItemStack handleTransferPost(TileEntityLockableLoot te, int i, ItemStack stack, Object destination, ItemStack remainder) {
        // For Hopper this is always empty when transfer worked
        // For Droppers the remainder is one less than the original stack
        if (remainder.isEmpty() || remainder.getCount() == stack.getCount() - 1) {
            captureInsertOrigin(te, i, stack);
            Cause.Builder builder = Cause.source(te);

            if (!(destination instanceof Inventory)) {
                throw new IllegalStateException("Target Inventory is not an inventory"); // TODO forge inventory interface?
            }
            ChangeInventoryEvent.Transfer.Post event =
                    SpongeEventFactory.createChangeInventoryEventTransferPost(builder.build(), ((Inventory) te), ((Inventory) destination), ((IMixinInventory) te).getCapturedTransactions());

            SpongeImpl.postEvent(event);
            if (event.isCancelled()) {
                // restore inventories
                setSlots(event.getTransactions(), SlotTransaction::getOriginal);
                // For hoppers we set the remainder so vanilla thinks there was not enough place for the item and tries again
                // TODO do we want this?
                remainder = stack;
            } else {
                // handle custom inventory transaction result
                setSlots(event.getTransactions(), SlotTransaction::getFinal);
            }

            ((IMixinInventory) te).getCapturedTransactions().clear();
        }
        return remainder;
    }

    private static void setSlots(List<SlotTransaction> transactions, Function<SlotTransaction, ItemStackSnapshot> func) {
        transactions.forEach(t -> t.getSlot().set(func.apply(t).createStack()));
    }

    private static void captureInsertOrigin(TileEntityLockableLoot te, int i, ItemStack stack) {
        Slot slot = ((OrderedInventory) ((MinecraftInventoryAdapter) te).query(OrderedInventory.class)).getSlot(SlotIndex.of(i)).get();
        SlotTransaction trans = new SlotTransaction(slot,
                ItemStackUtil.snapshotOf(stack),
                ItemStackUtil.snapshotOf(slot.peek().orElse(org.spongepowered.api.item.inventory.ItemStack.empty())));
        ((IMixinInventory) te).getCapturedTransactions().add(trans);
    }

    public static ItemStack captureInsertRemote(IInventory source, InventoryAdapter destination, int index, Supplier<ItemStack> insert) {
        Slot slot = ((OrderedInventory) destination.query(OrderedInventory.class)).getSlot(SlotIndex.of(index)).get();
        ItemStackSnapshot from = slot.peek().map(ItemStackUtil::snapshotOf).orElse(ItemStackSnapshot.NONE);
        ItemStack remaining = insert.get();
        ItemStackSnapshot to = slot.peek().map(ItemStackUtil::snapshotOf).orElse(ItemStackSnapshot.NONE);

        ((IMixinInventory) source).getCapturedTransactions().add(new SlotTransaction(slot, from, to));

        return remaining;
    }
}
