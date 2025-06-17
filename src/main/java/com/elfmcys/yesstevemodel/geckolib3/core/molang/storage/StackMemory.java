package com.elfmcys.yesstevemodel.geckolib3.core.molang.storage;

import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.molang.runtime.Function;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class StackMemory implements ITempVariableStorage {
    private static final int MAX_STACK_DEPTH = 32;

    private Object[] mem = new Object[16];
    private int stackTopPointer = 0;

    private final IntArrayList stackFrameSize = new IntArrayList(4);
    private final IntArrayList argSize = new IntArrayList(4);
    private final ArgsAccessor argsAccessor = new ArgsAccessor();

    public StackMemory() {
        stackFrameSize.add(0);
        argSize.add(0);
    }

    private void ensureCapacity(int cap) {
        if (mem.length < cap) {
            var newCap = mem.length * 2;
            while (newCap < cap) {
                newCap *= 2;
            }
            mem = Arrays.copyOf(mem, newCap);
        }
    }

    private int getRealAddr(int addr) {
        var top = addr + 1;
        var offset = getVariableOffset();
        if (stackTopPointer < top) {
            stackTopPointer = top;
            ensureCapacity(offset + top);
        }
        return offset + addr;
    }

    public Object getTemp(int addr) {
        return mem[getRealAddr(addr)];
    }

    public void setTemp(int addr, Object value) {
        mem[getRealAddr(addr)] = value;
    }

    public boolean push(List<?> args) {
        if (stackFrameSize.size() < MAX_STACK_DEPTH) {
            stackFrameSize.add(stackTopPointer + getVariableOffset());
            argSize.add(args.size());
            stackTopPointer = 0;

            var offset = getArgsOffset();
            ensureCapacity(offset + args.size());
            for (int i = 0; i < args.size(); i++) {
                mem[offset + i] = args.get(i);
            }

            return true;
        }

        return false;
    }

    public boolean push(ExecutionContext<?> ctx, Function.ArgumentCollection args) {
        if (stackFrameSize.size() < MAX_STACK_DEPTH) {

            var offset = getVariableOffset() + stackTopPointer;
            ensureCapacity(offset + args.size());
            for (int i = 0; i < args.size(); i++) {
                mem[offset + i] = args.getValue(ctx, i);
            }

            stackFrameSize.add(offset);
            argSize.add(args.size());
            stackTopPointer = 0;

            return true;
        }

        return false;
    }

    public void pop() {
        if (stackFrameSize.size() > 1) {
            var topPointer = stackFrameSize.removeInt(stackFrameSize.size() - 1);
            argSize.removeInt(argSize.size() - 1);
            stackTopPointer = topPointer;
        }
    }

    public List<?> argsAccessor() {
        return argsAccessor;
    }

    public int getVariableOffset() {
        return getArgsOffset() + getArgsSize();
    }

    private int getArgsOffset() {
        return stackFrameSize.getInt(stackFrameSize.size() - 1);
    }

    private int getArgsSize() {
        return argSize.getInt(argSize.size() - 1);
    }

    class ArgsIterator implements Iterator<Object> {
        private int index = 0;

        @Override
        public boolean hasNext() {
            return index < getArgsSize();
        }

        @Override
        public Object next() {
            if (index < getArgsSize()) {
                return mem[getArgsOffset() + index++];
            }
            return null;
        }
    }

    class ArgsAccessor implements List<Object> {
        @Override
        public int size() {
            return getArgsSize();
        }

        @Override
        public boolean isEmpty() {
            return getArgsSize() == 0;
        }

        @Override
        public Object get(int index) {
            if (index >= 0 && index < getArgsSize()) {
                return mem[getArgsOffset() + index];
            }
            return null;
        }

        @Override
        public @NotNull Iterator<Object> iterator() {
            return new ArgsIterator();
        }

        @Override
        public boolean contains(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull Object @NotNull [] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull <T> T @NotNull [] toArray(@NotNull T[] a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(@NotNull Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(@NotNull Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(int index, @NotNull Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(@NotNull Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(@NotNull Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object set(int index, Object element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(int index, Object element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object remove(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int indexOf(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int lastIndexOf(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull ListIterator<Object> listIterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull ListIterator<Object> listIterator(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull List<Object> subList(int fromIndex, int toIndex) {
            throw new UnsupportedOperationException();
        }
    }
}
