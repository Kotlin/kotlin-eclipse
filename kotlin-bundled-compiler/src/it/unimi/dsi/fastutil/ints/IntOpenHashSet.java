package it.unimi.dsi.fastutil.ints;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;

public class IntOpenHashSet extends AbstractIntSet implements Hash, Serializable, Cloneable {
    private static final long serialVersionUID = 0L;
    protected transient int[] key;
    protected transient int mask;
    protected transient boolean containsNull;
    protected transient int n;
    protected transient int maxFill;
    protected final transient int minN;
    protected int size;
    protected final float f;

    public IntOpenHashSet(int expected, float f) {
        if (!(f <= 0.0F) && !(f >= 1.0F)) {
            if (expected < 0) {
                throw new IllegalArgumentException("The expected number of elements must be nonnegative");
            } else {
                this.f = f;
                this.minN = this.n = HashCommon.arraySize(expected, f);
                this.mask = this.n - 1;
                this.maxFill = HashCommon.maxFill(this.n, f);
                this.key = new int[this.n + 1];
            }
        } else {
            throw new IllegalArgumentException("Load factor must be greater than 0 and smaller than 1");
        }
    }

    public IntOpenHashSet(int expected) {
        this(expected, 0.75F);
    }

    public IntOpenHashSet() {
        this(16, 0.75F);
    }

    private int realSize() {
        return this.containsNull ? this.size - 1 : this.size;
    }

    private void ensureCapacity(int capacity) {
        int needed = HashCommon.arraySize(capacity, this.f);
        if (needed > this.n) {
            this.rehash(needed);
        }

    }

    private void tryCapacity(long capacity) {
        int needed = (int)Math.min(1073741824L, Math.max(2L, HashCommon.nextPowerOfTwo((long)Math.ceil((double)((float)capacity / this.f)))));
        if (needed > this.n) {
            this.rehash(needed);
        }

    }

    public boolean addAll(Collection<? extends Integer> c) {
        if ((double)this.f <= 0.5D) {
            this.ensureCapacity(c.size());
        } else {
            this.tryCapacity((long)(this.size() + c.size()));
        }

        return super.addAll(c);
    }

    public boolean add(int k) {
        if (k == 0) {
            if (this.containsNull) {
                return false;
            }

            this.containsNull = true;
        } else {
            int[] key = this.key;
            int pos;
            int curr;
            if ((curr = key[pos = HashCommon.mix(k) & this.mask]) != 0) {
                if (curr == k) {
                    return false;
                }

                while((curr = key[pos = pos + 1 & this.mask]) != 0) {
                    if (curr == k) {
                        return false;
                    }
                }
            }

            key[pos] = k;
        }

        if (this.size++ >= this.maxFill) {
            this.rehash(HashCommon.arraySize(this.size + 1, this.f));
        }

        return true;
    }

    protected final void shiftKeys(int pos) {
        int[] key = this.key;

        while(true) {
            int last = pos;
            pos = pos + 1 & this.mask;

            int curr;
            while(true) {
                if ((curr = key[pos]) == 0) {
                    key[last] = 0;
                    return;
                }

                int slot = HashCommon.mix(curr) & this.mask;
                if (last <= pos) {
                    if (last >= slot || slot > pos) {
                        break;
                    }
                } else if (last >= slot && slot > pos) {
                    break;
                }

                pos = pos + 1 & this.mask;
            }

            key[last] = curr;
        }
    }

    private boolean removeEntry(int pos) {
        --this.size;
        this.shiftKeys(pos);
        if (this.n > this.minN && this.size < this.maxFill / 4 && this.n > 16) {
            this.rehash(this.n / 2);
        }

        return true;
    }

    private boolean removeNullEntry() {
        this.containsNull = false;
        this.key[this.n] = 0;
        --this.size;
        if (this.n > this.minN && this.size < this.maxFill / 4 && this.n > 16) {
            this.rehash(this.n / 2);
        }

        return true;
    }

    public boolean remove(int k) {
        if (k == 0) {
            return this.containsNull ? this.removeNullEntry() : false;
        } else {
            int[] key = this.key;
            int curr;
            int pos;
            if ((curr = key[pos = HashCommon.mix(k) & this.mask]) == 0) {
                return false;
            } else if (k == curr) {
                return this.removeEntry(pos);
            } else {
                while((curr = key[pos = pos + 1 & this.mask]) != 0) {
                    if (k == curr) {
                        return this.removeEntry(pos);
                    }
                }

                return false;
            }
        }
    }

    public boolean contains(int k) {
        if (k == 0) {
            return this.containsNull;
        } else {
            int[] key = this.key;
            int curr;
            int pos;
            if ((curr = key[pos = HashCommon.mix(k) & this.mask]) == 0) {
                return false;
            } else if (k == curr) {
                return true;
            } else {
                while((curr = key[pos = pos + 1 & this.mask]) != 0) {
                    if (k == curr) {
                        return true;
                    }
                }

                return false;
            }
        }
    }

    public void clear() {
        if (this.size != 0) {
            this.size = 0;
            this.containsNull = false;
            Arrays.fill(this.key, 0);
        }
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public IntIterator iterator() {
        return new IntOpenHashSet.SetIterator();
    }

    protected void rehash(int newN) {
        int[] key = this.key;
        int mask = newN - 1;
        int[] newKey = new int[newN + 1];
        int i = this.n;

        int pos;
        for(int var7 = this.realSize(); var7-- != 0; newKey[pos] = key[i]) {
            do {
                --i;
            } while(key[i] == 0);

            if (newKey[pos = HashCommon.mix(key[i]) & mask] != 0) {
                while(newKey[pos = pos + 1 & mask] != 0) {
                }
            }
        }

        this.n = newN;
        this.mask = mask;
        this.maxFill = HashCommon.maxFill(this.n, this.f);
        this.key = newKey;
    }

    public IntOpenHashSet clone() {
        IntOpenHashSet c;
        try {
            c = (IntOpenHashSet)super.clone();
        } catch (CloneNotSupportedException var3) {
            throw new InternalError();
        }

        c.key = (int[])this.key.clone();
        c.containsNull = this.containsNull;
        return c;
    }

    public int hashCode() {
        int h = 0;
        int j = this.realSize();

        for(int i = 0; j-- != 0; ++i) {
            while(this.key[i] == 0) {
                ++i;
            }

            h += this.key[i];
        }

        return h;
    }

    private class SetIterator implements IntIterator {
        int pos;
        int last;
        int c;
        boolean mustReturnNull;
        IntArrayList wrapped;

        private SetIterator() {
            this.pos = IntOpenHashSet.this.n;
            this.last = -1;
            this.c = IntOpenHashSet.this.size;
            this.mustReturnNull = IntOpenHashSet.this.containsNull;
        }

        public boolean hasNext() {
            return this.c != 0;
        }

        public int nextInt() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            } else {
                --this.c;
                if (this.mustReturnNull) {
                    this.mustReturnNull = false;
                    this.last = IntOpenHashSet.this.n;
                    return IntOpenHashSet.this.key[IntOpenHashSet.this.n];
                } else {
                    int[] key = IntOpenHashSet.this.key;

                    while(--this.pos >= 0) {
                        if (key[this.pos] != 0) {
                            return key[this.last = this.pos];
                        }
                    }

                    this.last = -2147483648;
                    return this.wrapped.getInt(-this.pos - 1);
                }
            }
        }

        private final void shiftKeys(int pos) {
            int[] key = IntOpenHashSet.this.key;

            while(true) {
                int last = pos;
                pos = pos + 1 & IntOpenHashSet.this.mask;

                int curr;
                while(true) {
                    if ((curr = key[pos]) == 0) {
                        key[last] = 0;
                        return;
                    }

                    int slot = HashCommon.mix(curr) & IntOpenHashSet.this.mask;
                    if (last <= pos) {
                        if (last >= slot || slot > pos) {
                            break;
                        }
                    } else if (last >= slot && slot > pos) {
                        break;
                    }

                    pos = pos + 1 & IntOpenHashSet.this.mask;
                }

                if (pos < last) {
                    if (this.wrapped == null) {
                        this.wrapped = new IntArrayList(2);
                    }

                    this.wrapped.add(key[pos]);
                }

                key[last] = curr;
            }
        }

        public void remove() {
            if (this.last == -1) {
                throw new IllegalStateException();
            } else {
                if (this.last == IntOpenHashSet.this.n) {
                    IntOpenHashSet.this.containsNull = false;
                    IntOpenHashSet.this.key[IntOpenHashSet.this.n] = 0;
                } else {
                    if (this.pos < 0) {
                        IntOpenHashSet.this.remove(this.wrapped.getInt(-this.pos - 1));
                        this.last = -1;
                        return;
                    }

                    this.shiftKeys(this.last);
                }

                --IntOpenHashSet.this.size;
                this.last = -1;
            }
        }
    }
}