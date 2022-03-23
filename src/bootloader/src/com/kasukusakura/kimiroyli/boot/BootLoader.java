package com.kasukusakura.kimiroyli.boot;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.*;
import java.net.*;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;

public class BootLoader {
    @SuppressWarnings("ConstantConditions")
    public static void premain(String opts, Instrumentation instrumentation) throws Throwable {
        var resLoader = BootLoader.class.getClassLoader().getClass().getClassLoader();
        // com/kasukusakura/jvmsecurity/boot/lib
        var libraries = new ArrayList<String>();
        try (var libsReader = new BufferedReader(new InputStreamReader(
                resLoader.getResourceAsStream("com/kasukusakura/kimiroyli/boot/lib/libs.txt")
        ))) {
            while (true) {
                var l = libsReader.readLine();
                if (l == null) break;
                if (l.isBlank()) continue;
                libraries.add(l);
            }
        }
        var libsCL = new URLClassLoader(new URL[0], ClassLoader.getPlatformClassLoader()) {
            @Override
            protected void addURL(URL url) {
                super.addURL(url);
            }
        };
        var librariesClasses = new HashMap<String, byte[]>();

        // jsl://lib.jar/cls
        var urlHandler = new URLStreamHandler() {
            @Override
            protected void parseURL(URL u, String spec, int start, int limit) {
                super.parseURL(u, spec, start, limit);
                setURL(u,
                        u.getProtocol(),
                        u.getHost(),
                        u.getPort(),
                        u.getAuthority(),
                        u.getUserInfo(),
                        u.getPath(),
                        u.getQuery(),
                        u.getPath().startsWith("/") ? u.getPath().substring(1) : u.getPath()
                );
            }

            @Override
            protected String toExternalForm(URL u) {
                return "jsl://kimiroyli/" + u.getPath();
            }

            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                var p = librariesClasses.get(u.getRef());
                if (p == null) throw new FileNotFoundException(u.getRef());
                //System.out.println("URLHandler: loaded " + u.getRef());
                return new URLConnection(u) {
                    @Override
                    public void connect() {
                        connected = true;
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        connect();
                        return new ByteArrayInputStream(p);
                    }
                };
            }
        };
        for (var lib : libraries) {
            try (var zip = new ZipInputStream(new BufferedInputStream(
                    resLoader.getResourceAsStream("com/kasukusakura/kimiroyli/boot/lib/" + lib)
            ))) {
                while (true) {
                    var entry = zip.getNextEntry();
                    if (entry == null) break;
                    if (entry.isDirectory()) continue;
                    var name = entry.getName();
                    if (name.startsWith("/")) name = name.substring(1);
                    var ptx = "library-" + lib + '/' + name;
                    librariesClasses.put(ptx, zip.readAllBytes());
                }
                libsCL.addURL(new URL(null, "jsl://kimiroyli/library-" + lib + "/", urlHandler));
            }
        }

        // librariesClasses.keySet().stream().sorted().forEach(System.out::println);

        var refs = new HashMap<String, ModuleReference>();


        regModule(resLoader, "unsafe-accessor.jar", "name kimiroyli.unsafe\nexports * -> kimiroyli.api, kimiroyli.core", refs, urlHandler, librariesClasses);
        regModule(resLoader, "api.jar", null, refs, urlHandler, librariesClasses);
        regModule(resLoader, "system.jar", null, refs, urlHandler, librariesClasses);

        var finder = new ModuleFinder() {
            @Override
            public Optional<ModuleReference> find(String name) {
                return Optional.ofNullable(refs.get(name));
            }

            @Override
            public Set<ModuleReference> findAll() {
                return null;
            }
        };
        var bl = ModuleLayer.boot();
        var newConf = bl.configuration().resolve(finder, ModuleFinder.ofSystem(), new HashSet<>(refs.keySet()));

        var coreCL = new MLoader(newConf, libsCL);

        var controller = ModuleLayer.defineModules(newConf, List.of(bl), $ -> coreCL);
        var coreModule = controller.layer().findModule("kimiroyli.core").orElseThrow();
        BootLoader.class.getModule().addReads(coreModule);
        for (var mod : controller.layer().modules()) {
            controller.addReads(mod, libsCL.getUnnamedModule());
        }
        controller.addExports(coreModule, "com.kasukusakura.kimiroyli.core", BootLoader.class.getModule());

        var bootstrap = Class.forName(coreModule, "com.kasukusakura.kimiroyli.core.Bootstrap");
        MethodHandles.lookup().findStatic(
                bootstrap, "premain", MethodType.methodType(void.class,
                        String.class, Instrumentation.class,
                        ModuleLayer.Controller.class,
                        Class.class
                )
        ).invokeExact(opts, instrumentation, controller, BootLoader.class);
    }

    static class ModuleRef extends ModuleReference {
        final Map<String, byte[]> image;
        final URL base;

        ModuleRef(ModuleDescriptor descriptor, Map<String, byte[]> image, URL url, Map<String, byte[]> classes) {
            super(descriptor, null);
            this.image = image;
            base = url;
        }

        @Override
        public ModuleReader open() throws IOException {
            throw new IOException();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static Map<String, byte[]> readImage(ClassLoader cl, String coreLibName, Map<String, byte[]> classes) throws Throwable {
        var rsp = new HashMap<String, byte[]>();
        try (var zip = new ZipInputStream(new BufferedInputStream(
                cl.getResourceAsStream("com/kasukusakura/kimiroyli/boot/core/" + coreLibName)
        ))) {
            while (true) {
                var entry = zip.getNextEntry();
                if (entry == null) break;
                if (entry.isDirectory()) continue;
                var name = entry.getName();
                if (name.startsWith("/")) name = name.substring(1);
                var data = zip.readAllBytes();
                rsp.put(name, data);
                classes.put("core-" + coreLibName + "/" + name, data);
            }
        }
        return rsp;
    }

    private static Set<String> pkgs(Map<String, byte[]> image, String pn) {
        if (pn.endsWith("*")) {
            pn = pn.substring(0, pn.length() - 1).replace('.', '/');
            var rsp = new HashSet<String>();
            for (var key : image.keySet()) {
                if (key.endsWith(".class") && !key.startsWith("META-INF")) {
                    if (key.startsWith(pn)) {
                        var cn = key.substring(0, key.length() - 6);
                        cn = cn.substring(0, cn.lastIndexOf('/'));
                        rsp.add(cn.replace('/', '.'));
                    }
                }
            }
            return rsp;
        } else return Set.of(pn);
    }

    private static void regModule(
            ClassLoader cl,
            String coreLibName,
            String moduleTxt,
            Map<String, ModuleReference> refs,
            URLStreamHandler handler,
            Map<String, byte[]> classes
    ) throws Throwable {
        var image = readImage(cl, coreLibName, classes);
        var url = new URL(null, "jsl://kimiroyli/core-" + coreLibName + "/", handler);

        try (var reader = new BufferedReader(moduleTxt != null
                ? new StringReader(moduleTxt)
                : new InputStreamReader(new ByteArrayInputStream(image.get("META-INF/module.txt")))
        )) {
            ModuleDescriptor.Builder moduleDesc;
            {
                var fline = reader.readLine();
                if (fline.startsWith("name ")) {
                    moduleDesc = ModuleDescriptor.newModule(fline.substring(5).strip());
                } else {
                    throw new IllegalStateException("module.txt not starting with `name`");
                }

                var a = new HashSet<String>();
                var opens = new HashMap<String, Set<String>>();
                var exports = new HashMap<String, Set<String>>();
                var services = new HashMap<String, List<String>>();

                while (true) {
                    var cmd = reader.readLine();
                    if (cmd == null) break;
                    if (cmd.isBlank()) continue;
                    if (cmd.startsWith("require ")) {
                        moduleDesc.requires(cmd.substring(8).strip());
                    } else if (cmd.startsWith("exports ") || cmd.startsWith("open ")) {
                        var open = cmd.startsWith("open ");
                        var argx = cmd.substring(open ? 5 : 8).strip();
                        var idx = argx.indexOf("->");
                        if (idx != -1) {
                            var targets = Stream.of(
                                    argx.substring(idx + 3).split(",")
                            ).map(String::strip).collect(Collectors.toSet());
                            var pkgs = pkgs(image, argx.substring(0, idx).strip());
                            for (var pn : pkgs) {
                                if (open) {
                                    opens.put(pn, targets);
                                } else {
                                    exports.put(pn, targets);
                                }
                            }
                        } else {
                            for (var pn : pkgs(image, argx)) {
                                if (open) {
                                    opens.put(pn, a);
                                } else {
                                    exports.put(pn, a);
                                }
                            }
                        }
                    } else if (cmd.startsWith("cancel ")) {
                        var argx = cmd.substring(7).strip();
                        for (var pn : pkgs(image, argx)) {
                            opens.remove(pn);
                            exports.remove(pn);
                        }
                    } else if (cmd.startsWith("uses ")) {
                        moduleDesc.uses(cmd.substring(5).strip());
                    } else if (cmd.startsWith("provider ")) {
                        var argx = cmd.substring(9).strip();
                        var idx = argx.indexOf("->");
                        if (idx == -1) throw new IllegalArgumentException(cmd);
                        services.computeIfAbsent(argx.substring(0, idx).strip(), $ -> new ArrayList<>())
                                .add(argx.substring(idx + 2).strip());
                    } else {
                        throw new IllegalArgumentException("Unknown command: " + cmd);
                    }
                }

                for (var open : opens.entrySet()) {
                    if (open.getValue() == a) {
                        moduleDesc.opens(open.getKey());
                    } else {
                        moduleDesc.opens(open.getKey(), open.getValue());
                    }
                }
                for (var export : exports.entrySet()) {
                    if (export.getValue() == a) {
                        moduleDesc.exports(export.getKey());
                    } else {
                        moduleDesc.exports(export.getKey(), export.getValue());
                    }
                }
                for (var service: services.entrySet()) {
                    moduleDesc.provides(service.getKey(), service.getValue());
                }
            }
            {
                var pkgs = new HashSet<String>();
                for (var k : image.keySet()) {
                    if (k.endsWith(".class") && !k.startsWith("META-INF/")) {
                        var cname = k.substring(0, k.length() - 6);
                        cname = cname.substring(0, cname.lastIndexOf('/'));
                        pkgs.add(cname.replace('/', '.'));
                    }
                }
                moduleDesc.packages(pkgs);
            }
            var dsc = moduleDesc.build();
            refs.put(dsc.name(), new ModuleRef(dsc, image, url, classes));
        }
    }

    private static class MLoader extends ClassLoader {
        private static class LoadedModule {
            final ModuleRef ref;
            public ProtectionDomain pd;

            private LoadedModule(ModuleRef ref) {
                this.ref = ref;
            }

            @Override
            public String toString() {
                return "LoadedModule{" +
                        "ref=" + ref.descriptor().name() +
                        ", pd=" + pd +
                        '}';
            }
        }

        private final Map<String, LoadedModule> pkg2mod = new HashMap<>();
        private final Map<String, LoadedModule> name2mod = new HashMap<>();

        private static String pkg(String cname) {
            var idx = cname.lastIndexOf('/');
            if (idx != -1) return cname.substring(0, idx).replace('/', '.');
            idx = cname.lastIndexOf('.');
            if (idx == -1) return "";
            return cname.substring(0, idx);
        }

        MLoader(Configuration conf, ClassLoader parent) {
            super(parent);
            for (var module : conf.modules()) {
                var ref = (ModuleRef) module.reference();
                var mod = new LoadedModule(ref);
                name2mod.put(module.name(), mod);
                for (var pkg : ref.descriptor().packages()) {
                    pkg2mod.put(pkg, mod);
                }
            }
        }

        @Override
        protected URL findResource(String moduleName, String name) throws IOException {
            var mod = name2mod.get(moduleName);
            if (mod == null) return null;
            if (mod.ref.image.containsKey(name)) {
                return new URL(mod.ref.base, name);
            }
            return null;
        }

        @Override
        protected URL findResource(String name) {
            var pkg = pkg(name);
            var mod = pkg2mod.get(pkg);
            if (mod == null) {
                for (var m : name2mod.values()) {
                    if (m.ref.image.containsKey(name)) {
                        try {
                            return new URL(m.ref.base, name);
                        } catch (MalformedURLException ignored) {
                        }
                    }
                }
            }
            return null;
        }

        @Override
        protected Class<?> findClass(String moduleName, String name) {
            var mod = name2mod.get(moduleName);
            return findClassOrNull(mod, name);
        }

        private Class<?> findClassOrNull(LoadedModule module, String name) {
            if (module == null) return null;
            var data = module.ref.image.get(name.replace('.', '/') + ".class");
            if (data == null) return null;
            return defineClass(name, data, 0, data.length, module.pd);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            var pkg = pkg2mod.get(pkg(name));
            var c = findClassOrNull(pkg, name);
            if (c == null) throw new ClassNotFoundException(name);
            return c;
        }
    }
}
