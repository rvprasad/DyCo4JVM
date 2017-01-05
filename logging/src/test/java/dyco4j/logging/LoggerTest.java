/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 *
 */
package dyco4j.logging;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("ALL")
public final class LoggerTest {
    private StringWriter logStore;

    @Before
    public void setUp() throws Exception {
        logStore = new StringWriter();
        Logger.initialize(new PrintWriter(logStore));
    }

    @After
    public void tearDown() throws Exception {
        logStore.close();
    }

    @Test
    public void testLogArgument() throws Exception {
        final String _msg = "test message";
        final byte _idx = 1;
        Logger.logArgument(_idx, _msg);

        final String _expected = MessageFormat.format("{0},{1},{2},{3}", Thread.currentThread().getId(),
                Logger.METHOD_ARG_TAG, _idx, _msg);
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogGetArray() throws Exception {
        final String[] _array = new String[]{"array"};
        final String _value = "value";
        final int _idx = 1;
        final Logger.ArrayAction _action = Logger.ArrayAction.GETA;
        Logger.logArray(_array, _idx, _value, _action);

        final String _expected = MessageFormat
                .format("{0},{1},{2},{3},{4}", Thread.currentThread().getId(), _action, _idx,
                        Logger.toString(_array), _value);
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogPutArray() throws Exception {
        final String[] _array = new String[]{"array"};
        final String _value = "value";
        final int _idx = 1;
        final Logger.ArrayAction _action = Logger.ArrayAction.PUTA;
        Logger.logArray(_array, _idx, _value, _action);

        final String _expected = MessageFormat
                .format("{0},{1},{2},{3},{4}", Thread.currentThread().getId(), _action, _idx,
                        Logger.toString(_array), _value);
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogException() throws Exception {
        final Exception _tmp = new RuntimeException();
        Logger.logException(_tmp);

        final String _expected = MessageFormat
                .format("{0},{1},{2},{3}", Thread.currentThread().getId(), Logger.METHOD_EXCEPTION_TAG,
                        Logger.toString(_tmp), _tmp.getClass().getName());
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogFieldForInstanceField() throws Exception {
        final Object _object = 10;
        final String _fieldValue = "test";
        final String _fieldName = "message";
        final Logger.FieldAction _action = Logger.FieldAction.GETF;
        Logger.logField(_object, _fieldValue, _fieldName, _action);

        final String _expected = MessageFormat
                .format("{0},{1},{2},{3},{4}", Thread.currentThread().getId(), _action, _fieldName,
                        Logger.toString(_object), _fieldValue);
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogFieldForStaticField() throws Exception {
        final String _fieldValue = "test";
        final String _fieldName = "message";
        final Logger.FieldAction _action = Logger.FieldAction.PUTF;
        Logger.logField(null, _fieldValue, _fieldName, _action);

        final String _expected = MessageFormat
                .format("{0},{1},{2},,{3}", Thread.currentThread().getId(), _action, _fieldName, _fieldValue);
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogMethodCall() throws Exception {
        final String _msg = "test message";
        Logger.logMethodCall(_msg);

        final String _expected = MessageFormat.format("{0},{1},{2}", Thread.currentThread().getId(),
                Logger.METHOD_CALL_TAG, _msg);
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogMethodEntry() throws Exception {
        final String _msg = "test message";
        Logger.logMethodEntry(_msg);

        final String _expected = MessageFormat.format("{0},{1},{2}", Thread.currentThread().getId(),
                Logger.METHOD_ENTRY_TAG, _msg);
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogMethodExitNormal() throws Exception {
        final String _msg = "test message";
        Logger.logMethodExit(_msg, "N");

        final String _expected = MessageFormat.format("{0},{1},{2},N", Thread.currentThread().getId(),
                Logger.METHOD_EXIT_TAG, _msg);
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogMethodExitExceptional() throws Exception {
        final String _msg = "test message";
        Logger.logMethodExit(_msg, "E");

        final String _expected = MessageFormat.format("{0},{1},{2},E", Thread.currentThread().getId(),
                Logger.METHOD_EXIT_TAG, _msg);
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogReturnForNonVoidValue() throws Exception {
        final String _msg = "test message";
        Logger.logReturn(_msg);

        final String _expected = MessageFormat.format("{0},{1},{2}", Thread.currentThread().getId(),
                Logger.METHOD_RETURN_TAG, _msg);
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogReturnForVoidValue() throws Exception {
        Logger.logReturn(null);

        final String _expected = MessageFormat.format("{0},{1}", Thread.currentThread().getId(),
                Logger.METHOD_RETURN_TAG);
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogStringForMultipleLogStmts() throws Exception {
        final String _msg1 = "test message 1";
        final String _msg2 = "test message 2";
        Logger.log(_msg1);
        Logger.log(_msg2);

        final String[] _tmp = getContent();
        final String _expected1 = MessageFormat.format("{0},{1}", Thread.currentThread().getId(), _msg1);
        assertEquals(_expected1, _tmp[1]);

        final String _expected2 = MessageFormat.format("{0},{1}", Thread.currentThread().getId(), _msg2);
        assertEquals(_expected2, _tmp[2]);
    }

    @Test
    public void testLogStringForOneLogStmt() throws Exception {
        final String _msg = "test message";
        Logger.log(_msg);

        final String _expected = MessageFormat.format("{0},{1}", Thread.currentThread().getId(), _msg);
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogStringForIdenticalLogStmts() throws Exception {
        final String _msg1 = "test message 1";
        Logger.log(_msg1);
        Logger.log(_msg1);
        Logger.log(_msg1);
        final String _msg2 = "test message 2";
        Logger.log(_msg2);
        Logger.log(_msg2);
        Logger.cleanupForTest();

        final String[] _tmp1 = getContent();
        final String _expected1 = MessageFormat.format("{0},{1}", Thread.currentThread().getId(), _msg1);
        assertEquals(_expected1, _tmp1[1]);
        final String _expected2 = MessageFormat.format("{0},{1},2", Thread.currentThread().getId(), _msg1);
        assertEquals(_expected2, _tmp1[2]);
        final String _expected3 = MessageFormat.format("{0},{1}", Thread.currentThread().getId(), _msg2);
        assertEquals(_expected3, _tmp1[3]);
        final String _expected4 = MessageFormat.format("{0},{1},1", Thread.currentThread().getId(), _msg2);
        assertEquals(_expected4, _tmp1[4]);
    }

    @Test
    public void testLogVarArgsForMultipleLogStmts() throws Exception {
        final String[] _msg1 = new String[]{"test", "message", "1"};
        Logger.log(_msg1);

        final String[] _msg2 = new String[]{"test", "message", "2"};
        Logger.log(_msg2);

        final String[] _tmp = getContent();
        @SuppressWarnings("ConfusingArgumentToVarargsMethod") final String _expected1 =
                MessageFormat.format("{0},{1}", Thread.currentThread().getId(), String.join(",", _msg1));
        assertEquals(_expected1, _tmp[1]);

        @SuppressWarnings("ConfusingArgumentToVarargsMethod") final String _expected2 =
                MessageFormat.format("{0},{1}", Thread.currentThread().getId(), String.join(",", _msg2));
        assertEquals(_expected2, _tmp[2]);
    }

    @Test
    public void testLogVarArgsForOneLogStmt() throws Exception {
        final String[] _msg = new String[]{"test", "message"};
        Logger.log(_msg);

        @SuppressWarnings("ConfusingArgumentToVarargsMethod") final String _expected =
                MessageFormat.format("{0},{1}", Thread.currentThread().getId(), String.join(",", _msg));
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testToStringWithBoolean() throws Exception {
        final boolean _tmp1 = true;
        assertTrue(Logger.toString(_tmp1).matches(MessageFormat.format("^{0}", Logger.TRUE_VALUE)));

        final boolean _tmp2 = false;
        assertTrue(Logger.toString(_tmp2).matches(MessageFormat.format("^{0}", Logger.FALSE_VALUE)));

        assertNotEquals(Logger.toString(_tmp1), Logger.toString(_tmp2));
    }

    @Test
    public void testToStringWithByte() throws Exception {
        final byte _tmp = 10;
        assertTrue(Logger.toString(_tmp).matches(MessageFormat.format("^{0}.*", Logger.BYTE_TYPE_TAG)));
        assertEquals(Logger.toString(_tmp), Logger.toString(_tmp));
        assertNotEquals(Logger.toString(_tmp), Logger.toString(20));
    }

    @Test
    public void testToStringWithChar() throws Exception {
        final char _tmp = 'c';
        assertTrue(Logger.toString(_tmp).matches(MessageFormat.format("^{0}.*", Logger.CHAR_TYPE_TAG)));
        assertEquals(Logger.toString(_tmp), Logger.toString(_tmp));
        assertNotEquals(Logger.toString(_tmp), Logger.toString('d'));
    }

    @Test
    public void testToStringWithDouble() throws Exception {
        final double _tmp = 10.3d;
        assertTrue(Logger.toString(_tmp).matches(MessageFormat.format("^{0}.*", Logger.DOUBLE_TYPE_TAG)));
        assertEquals(Logger.toString(_tmp), Logger.toString(_tmp));
        assertNotEquals(Logger.toString(_tmp), Logger.toString(10.4d));
    }

    @Test
    public void testToStringWithFloat() throws Exception {
        final float _tmp = 10.3f;
        assertTrue(Logger.toString(_tmp).matches(MessageFormat.format("^{0}.*", Logger.FLOAT_TYPE_TAG)));
        assertEquals(Logger.toString(_tmp), Logger.toString(_tmp));
        assertNotEquals(Logger.toString(_tmp), Logger.toString(10.4f));
    }

    @Test
    public void testToStringWithInt() throws Exception {
        final int _tmp = 10;
        assertTrue(Logger.toString(_tmp).matches(MessageFormat.format("^{0}.*", Logger.INT_TYPE_TAG)));
        assertEquals(Logger.toString(_tmp), Logger.toString(_tmp));
        assertNotEquals(Logger.toString(_tmp), Logger.toString(20));
    }

    @Test
    public void testToStringWithLong() throws Exception {
        final long _tmp = 10L;
        assertTrue(Logger.toString(_tmp).matches(MessageFormat.format("^{0}.*", Logger.LONG_TYPE_TAG)));
        assertEquals(Logger.toString(_tmp), Logger.toString(_tmp));
        assertNotEquals(Logger.toString(_tmp), Logger.toString(20L));
    }

    @Test
    public void testToStringWithObjectForArray() throws Exception {
        final String[] _tmp = new String[]{"array"};
        assertTrue(Logger.toString(_tmp).matches(MessageFormat.format("^{0}.*", Logger.ARRAY_TYPE_TAG)));
        assertEquals(Logger.toString(_tmp), Logger.toString(_tmp));
        assertNotEquals(Logger.toString(_tmp), Logger.toString(new Object[]{"array"}));
    }

    @Test
    public void testToStringWithObjectForNull() throws Exception {
        assertEquals(Logger.NULL_VALUE, Logger.toString(null));
        assertNotEquals(Logger.NULL_VALUE, Logger.toString("null"));
    }

    @Test
    public void testToStringWithObjectForObject() throws Exception {
        final Class<?> _tmp = Logger.class;
        assertTrue(Logger.toString(_tmp).matches(MessageFormat.format("^{0}.*", Logger.OBJECT_TYPE_TAG)));
        assertEquals(Logger.toString(_tmp), Logger.toString(_tmp));
        assertNotEquals(Logger.toString(_tmp), Logger.toString(Integer.valueOf(4)));
    }

    @Test
    public void testToStringWithObjectForString() throws Exception {
        final String _tmp = "string";
        assertTrue(Logger.toString(_tmp).matches(MessageFormat.format("^{0}.*", Logger.STRING_TYPE_TAG)));
        assertEquals(Logger.toString(_tmp), Logger.toString(_tmp));
        assertNotEquals(Logger.toString(_tmp), Logger.toString("str"));
    }

    @Test
    public void testToStringWithObjectForThrowable() throws Exception {
        final Exception _tmp = new RuntimeException();
        assertTrue(Logger.toString(_tmp).matches(MessageFormat.format("^{0}.*", Logger.THROWABLE_TYPE_TAG)));
        assertEquals(Logger.toString(_tmp), Logger.toString(_tmp));
        assertNotEquals(Logger.toString(_tmp), Logger.toString(new IllegalStateException()));
    }

    @Test
    public void testToStringWithShort() throws Exception {
        final short _tmp = 10;
        assertTrue(Logger.toString(_tmp).matches(MessageFormat.format("^{0}.*", Logger.SHORT_TYPE_TAG)));
        assertEquals(Logger.toString(_tmp), Logger.toString(_tmp));
        assertNotEquals(Logger.toString(_tmp), Logger.toString((short) 20));
    }

    private String[] getContent() {
        return logStore.toString().split(System.lineSeparator());
    }
}
