package cz.habarta.typescript.generator;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.junit.*;


public class GenericsTest {

    @Test
    public void testDefaultGenerics() throws Exception {
        final Settings settings = TestUtils.settings();
        settings.customTypeProcessor = new DefaultTypeProcessor();

        final StringWriter stringWriter = new StringWriter();
        new TypeScriptGenerator(settings).generateTypeScript(Input.from(A.class), Output.to(stringWriter));
        final String actual = stringWriter.toString().trim();
        final String nl = settings.newline;
        final String expected =
                "interface A {" + nl +
                "    x: A;" + nl +
                "    y: A;" + nl +
                "    z: A;" + nl +
                "}";
        assertEquals(expected, actual);
    }

    @Test
    public void testAdvancedGenerics() throws Exception {
        final Settings settings = TestUtils.settings();
        settings.addTypeNamePrefix = "I";

        final StringWriter stringWriter = new StringWriter();
        new TypeScriptGenerator(settings).generateEmbeddableTypeScript(Input.from(A.class), Output.to(stringWriter), true, 0);
        final String actual = stringWriter.toString().trim();
        final String nl = settings.newline;
        final String expected =
                "export interface IA<U, V> {" + nl +
                "    x: IA<string, string>;" + nl +
                "    y: IA<IA<string, IB>, string[]>;" + nl +
                "    z: IA<{ [index: string]: V }, number[]>;" + nl +
                "}" + nl +
                "" + nl +
                "export interface IB {" + nl +
                "}";
        assertEquals(expected, actual);
        final ModelCompiler compiler = new TypeScriptGenerator(settings).getModelCompiler();
        assertEquals("IA<string, string>", compiler.typeFromJava(A.class.getField("x").getGenericType()).toString());
        assertEquals("IA<IA<string, IB>, string[]>", compiler.typeFromJava(A.class.getField("y").getGenericType()).toString());
        assertEquals("IA<{ [index: string]: V }, number[]>", compiler.typeFromJava(A.class.getField("z").getGenericType()).toString());
    }

    @Test
    public void testWildcardGeneric() {
        final Settings settings = TestUtils.settings();
        settings.addTypeNamePrefix = "I";
        settings.customTypeProcessor = new GenericsTypeProcessor();

        final StringWriter stringWriter = new StringWriter();
        new TypeScriptGenerator(settings).generateEmbeddableTypeScript(Input.from(C.class), Output.to(stringWriter), true, 0);
        final String actual = stringWriter.toString().trim();
        final String nl = settings.newline;
        final String expected =
                "export interface IC {" + nl +
                "    x: string[];" + nl +
                "}";
        System.out.println(actual);
        assertEquals(expected, actual);
    }

    class A<U,V> {
        public A<String, String> x;
        public A<A<String, B>, List<String>> y;
        public A<Map<String, V>, Set<Integer>> z;
    }

    class B {
    }

    class C {
        public List<? extends String> x;
    }
}
