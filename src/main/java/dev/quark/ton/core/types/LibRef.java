package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.boc.Writable;

import java.util.Arrays;
import java.util.Objects;

/**
 * TL-B:
 *  libref_hash$0 lib_hash:bits256 = LibRef;
 *  libref_ref$1  library:^Cell   = LibRef;
 */
public sealed interface LibRef extends Writable permits LibRef.LibRefHash, LibRef.LibRefRef {

    static LibRef load(Slice slice) {
        long t = slice.loadUint(1);
        if (t == 0) {
            byte[] hash = slice.loadBuffer(32);
            return new LibRefHash(hash);
        } else if (t == 1) {
            Cell library = slice.loadRef();
            return new LibRefRef(library);
        } else {
            // Формально uint1 -> других значений не бывает, но пусть будет защита
            throw new IllegalArgumentException("Invalid LibRef tag: " + t);
        }
    }

    static Writable store(LibRef src) {
        Objects.requireNonNull(src, "src");
        return src::writeTo;
    }

    // ----------------------------
    // Variants
    // ----------------------------

    final class LibRefHash implements LibRef {
        private final byte[] libHash; // 32 bytes

        public LibRefHash(byte[] libHash) {
            if (libHash == null || libHash.length != 32) {
                throw new IllegalArgumentException("libHash must be 32 bytes");
            }
            this.libHash = libHash.clone();
        }

        public byte[] libHash() {
            return libHash.clone();
        }

        @Override
        public void writeTo(Builder builder) {
            builder.storeUint(0, 1);
            builder.storeBuffer(libHash);
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof LibRefHash other) && Arrays.equals(libHash, other.libHash);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(libHash);
        }
    }

    final class LibRefRef implements LibRef {
        private final Cell library;

        public LibRefRef(Cell library) {
            this.library = Objects.requireNonNull(library, "library");
        }

        public Cell library() {
            return library;
        }

        @Override
        public void writeTo(Builder builder) {
            builder.storeUint(1, 1);
            builder.storeRef(library);
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof LibRefRef other) && Objects.equals(library, other.library);
        }

        @Override
        public int hashCode() {
            return Objects.hash(library);
        }
    }
}
