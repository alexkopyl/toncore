package dev.quark.ton.core.contract;

import dev.quark.ton.core.address.Address;
import dev.quark.ton.core.boc.Cell;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Objects;

/**
 * Port of ton-core/src/contract/openContract.ts
 * Works when:
 * - T is an INTERFACE type
 * - underlying contract implementation has methods getsend
 * that accept
 *   (ContractProvider, ...args)
 * - the opened proxy interface has getsendWITHOUT provider param.
 * If later your Contract in Java is designed differently, we'll adjust this file
 * when you send Contract / ContractProvider / ContractState / ContractABI.
 */
        public final class OpenContract {

            private OpenContract() {
            }

            @FunctionalInterface
            public interface ProviderFactory {
                ContractProvider create(Address address, Init init);
            }

            public static final class Init {
                public final Cell code;
                public final Cell data;

                public Init(Cell code, Cell data) {
                    this.code = code;
                    this.data = data;
                }
            }

            /**
             * @param src         contract implementation (must have address/init fields accessible via Contract)
             * @param openedIface interface that represents "OpenedContract<T>" (methods without provider)
             */
            @SuppressWarnings("unchecked")
            public static <T extends Contract> T openContract(
                    T src,
                    Class<T> openedIface,
                    ProviderFactory factory
            ) {
                Objects.requireNonNull(src, "src");
                Objects.requireNonNull(openedIface, "openedIface");
                Objects.requireNonNull(factory, "factory");

                // Resolve parameters (TS validation)
                Address address = src.address();
                if (address == null || !Address.isAddress(address)) {
                    throw new IllegalArgumentException("Invalid address");
                }

                Init init = null;
                Contract.StateInit srcInit = src.init();
                if (srcInit != null) {
                    if (!(srcInit.code() instanceof Cell)) {
                        throw new IllegalArgumentException("Invalid init.code");
                    }
                    if (!(srcInit.data() instanceof Cell)) {
                        throw new IllegalArgumentException("Invalid init.data");
                    }
                    init = new Init(srcInit.code(), srcInit.data());
                }

                // Create executor/provider
                ContractProvider executor = factory.create(address, init);

                // Create proxy
                InvocationHandler handler = (proxy, method, args) -> {
                    String name = method.getName();
                    Object[] safeArgs = args == null ? new Object[0] : args;

                    // If get*/send* -> call underlying method with (executor, ...args)
                    if (name.startsWith("get") || name.startsWith("send")) {
                        Method target = findTargetWithProvider(src.getClass(), name, safeArgs);
                        if (target != null) {
                            Object[] callArgs = new Object[safeArgs.length + 1];
                            callArgs[0] = executor;
                            System.arraycopy(safeArgs, 0, callArgs, 1, safeArgs.length);
                            target.setAccessible(true);
                            return target.invoke(src, callArgs);
                        }
                    }

                    // Otherwise: call same method on src (if exists)
                    try {
                        Method direct = src.getClass().getMethod(name, method.getParameterTypes());
                        direct.setAccessible(true);
                        return direct.invoke(src, safeArgs);
                    } catch (NoSuchMethodException e) {
                        // fallback to method on interface (might be default)
                        return method.invoke(src, safeArgs);
                    }
                };

                return (T) Proxy.newProxyInstance(
                        openedIface.getClassLoader(),
                        new Class<?>[]{openedIface},
                        handler
                );
            }

            private static Method findTargetWithProvider(Class<?> implClass, String name, Object[] args) {
                Method[] methods = implClass.getMethods();
                for (Method m : methods) {
                    if (!m.getName().equals(name)) continue;

                    Class<?>[] pt = m.getParameterTypes();
                    if (pt.length != args.length + 1) continue;
                    if (!ContractProvider.class.isAssignableFrom(pt[0])) continue;

                    // Check remaining params are compatible
                    boolean ok = true;
                    for (int i = 0; i < args.length; i++) {
                        Object a = args[i];
                        Class<?> expected = pt[i + 1];
                        if (a == null) {
                            // null is ok for non-primitive
                            if (expected.isPrimitive()) { ok = false; break; }
                        } else if (!wrap(expected).isAssignableFrom(a.getClass())) {
                            ok = false; break;
                        }
                    }

                    if (ok) return m;
                }
                return null;
            }

            private static Class<?> wrap(Class<?> c) {
                if (!c.isPrimitive()) return c;
                if (c == int.class) return Integer.class;
                if (c == long.class) return Long.class;
                if (c == boolean.class) return Boolean.class;
                if (c == byte.class) return Byte.class;
                if (c == short.class) return Short.class;
                if (c == char.class) return Character.class;
                if (c == float.class) return Float.class;
                if (c == double.class) return Double.class;
                return c;
            }
        }
