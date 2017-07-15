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
package org.spongepowered.common.item.inventory.query;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.query.QueryOperation;
import org.spongepowered.common.item.inventory.EmptyInventoryImpl;
import org.spongepowered.common.item.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.inventory.lens.Fabric;
import org.spongepowered.common.item.inventory.lens.Lens;
import org.spongepowered.common.item.inventory.lens.MutableLensSet;
import org.spongepowered.common.item.inventory.lens.impl.collections.MutableLensSetImpl;
import org.spongepowered.common.item.inventory.query.result.MinecraftResultAdapterProvider;
import org.spongepowered.common.item.inventory.query.result.QueryResult;

import java.util.Collection;

public class Query<TInventory, TStack> {

    public interface ResultAdapterProvider<TInventory, TStack> {

        QueryResult<TInventory, TStack> getResultAdapter(Fabric<TInventory> inventory, MutableLensSet<TInventory, TStack> matches, Inventory parent);

    }

    private static ResultAdapterProvider<?, ?> defaultResultProvider = new MinecraftResultAdapterProvider();

    private final InventoryAdapter<TInventory, TStack> adapter;

    private final Fabric<TInventory> inventory;

    private final Lens<TInventory, TStack> lens;

    private final QueryOperation[] queries;

    private Query(InventoryAdapter<TInventory, TStack> adapter, QueryOperation[] queries) {
        this.adapter = adapter;
        this.inventory = adapter.getInventory();
        this.lens = adapter.getRootLens();
        this.queries = queries;
    }

    @SuppressWarnings("unchecked")
    public Inventory execute() {
        return this.execute((ResultAdapterProvider<TInventory, TStack>) Query.defaultResultProvider);
    }

    public Inventory execute(ResultAdapterProvider<TInventory, TStack> resultProvider) {
        if (this.matches(this.lens, null, this.inventory)) {
            return this.lens.getAdapter(this.inventory, null);
        }

        return this.toResult(resultProvider, this.depthFirstSearch(this.lens));
    }

    @SuppressWarnings("unchecked")
    private Inventory toResult(ResultAdapterProvider<TInventory, TStack> resultProvider, MutableLensSet<TInventory, TStack> matches) {
        if (matches.isEmpty()) {
            return new EmptyInventoryImpl(this.adapter);
        }
        if (matches.size() == 1) {
            return matches.getLens(0).getAdapter(this.inventory, this.adapter);
        }

        if (resultProvider != null) {
            return resultProvider.getResultAdapter(this.inventory, matches, this.adapter);
        }

        return ((ResultAdapterProvider<TInventory, TStack>)Query.defaultResultProvider).getResultAdapter(this.inventory, matches, this.adapter);
    }

    private MutableLensSet<TInventory, TStack> depthFirstSearch(Lens<TInventory, TStack> lens) {
        MutableLensSet<TInventory, TStack> matches = new MutableLensSetImpl<TInventory, TStack>(true);

        for (Lens<TInventory, TStack> child : lens.getChildren()) {
            if (child == null) {
                continue;
            }
            if (!child.getChildren().isEmpty()) {
                matches.addAll(this.depthFirstSearch(child));
            }
            if (this.matches(child, lens, this.inventory)) {
                matches.add(child);
            }
        }

        // Only a single match or no matches
        if (matches.size() < 2) {
            return matches;
        }

        return this.reduce(lens, matches);
    }

    private boolean matches(Lens<TInventory, TStack> lens, Lens<TInventory, TStack> parent, Fabric<TInventory> inventory) {
        for (QueryOperation operation : this.queries) {
            if (((SpongeQueryOperation) operation).matches(lens, parent, inventory)) {
                return true;
            }
        }
        return false;
    }

    private MutableLensSet<TInventory, TStack> reduce(Lens<TInventory, TStack> lens, MutableLensSet<TInventory, TStack> matches) {
        if (lens.getSlots().equals(this.getSlots(matches))) {
            matches.clear();
            matches.add(lens);
            return matches;
        }

        for (Lens<TInventory, TStack> child : lens.getChildren()) {
            if (child == null || !child.isSubsetOf(matches)) {
                continue;
            }
            matches.removeAll(child.getChildren());
            matches.add(child);
        }

        return matches;
    }

    private IntSet getSlots(Collection<Lens<TInventory, TStack>> lenses) {
        IntSet slots = new IntOpenHashSet();
        for (Lens<TInventory, TStack> lens : lenses) {
            slots.addAll(lens.getSlots());
        }
        return slots;
    }

    public static <TInventory, TStack> Query<TInventory, TStack> compile(InventoryAdapter<TInventory, TStack> adapter, QueryOperation... queries) {
        return new Query<>(adapter, queries);
    }

    public static void setDefaultResultProvider(ResultAdapterProvider<?, ?> defaultResultProvider) {
        Query.defaultResultProvider = defaultResultProvider;
    }

}
