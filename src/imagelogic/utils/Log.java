package imagelogic.utils;

/**
 * log类
 * 
 * @author LeeFranker
 * 
 */
public class Log {

	private static final boolean PRINT_LOG = true;
	private static final int LEVEL = android.util.Log.VERBOSE;

	public static void v(String msgFormat) {
		if (PRINT_LOG && LEVEL <= android.util.Log.VERBOSE) {
			StackTraceElement ste = new Throwable().getStackTrace()[1];
			String fileName = ste.getFileName();
			String traceInfo = ste.getClassName() + "::";
			traceInfo += ste.getMethodName();
			traceInfo += "@" + ste.getLineNumber() + ">>>";
			android.util.Log.v(fileName, traceInfo + msgFormat);
		}
	}

	public static void v(String tag, String msgFormat) {

		if (PRINT_LOG && LEVEL <= android.util.Log.VERBOSE) {
			StackTraceElement ste = new Throwable().getStackTrace()[1];
			String traceInfo = ste.getClassName() + "::";
			traceInfo += ste.getMethodName();
			traceInfo += "@" + ste.getLineNumber() + ">>>";

			android.util.Log.v(tag, traceInfo + msgFormat);
		}
	}

	public static void v(String tag, String msgFormat, Throwable t) {
		if (PRINT_LOG && LEVEL <= android.util.Log.VERBOSE) {
			android.util.Log.v(tag, msgFormat, t);
		}
	}

	public static void d(String msgFormat) {
		if (PRINT_LOG && LEVEL <= android.util.Log.DEBUG) {
			StackTraceElement ste = new Throwable().getStackTrace()[1];
			String fileName = ste.getFileName();
			String traceInfo = ste.getClassName() + "::";
			traceInfo += ste.getMethodName();
			traceInfo += "@" + ste.getLineNumber() + ">>>";

			android.util.Log.v(fileName, traceInfo + msgFormat);
		}
	}

	public static void d(String tag, String msgFormat) {
		if (PRINT_LOG && LEVEL <= android.util.Log.DEBUG) {
			StackTraceElement ste = new Throwable().getStackTrace()[1];
			String traceInfo = ste.getClassName() + "::";
			traceInfo += ste.getMethodName();
			traceInfo += "@" + ste.getLineNumber() + ">>>";
			android.util.Log.d(tag, traceInfo + msgFormat);
		}
	}

	public static void d(String tag, String msgFormat, Throwable t) {
		if (PRINT_LOG && LEVEL <= android.util.Log.DEBUG) {
			android.util.Log.d(tag, msgFormat, t);
		}
	}

	public static void i(String tag, String msgFormat) {
		if (PRINT_LOG && LEVEL <= android.util.Log.INFO) {
			StackTraceElement ste = new Throwable().getStackTrace()[1];
			String traceInfo = ste.getFileName();
			traceInfo += "@" + ste.getLineNumber() + ">>>";
			android.util.Log.i(tag, traceInfo + msgFormat);
		}
	}

	public static void i(String tag, String msgFormat, Throwable t) {
		if (PRINT_LOG && LEVEL <= android.util.Log.INFO) {
			android.util.Log.i(tag, msgFormat, t);
		}
	}

	public static void w(String tag, String msgFormat) {
		if (PRINT_LOG && LEVEL <= android.util.Log.WARN) {
			StackTraceElement ste = new Throwable().getStackTrace()[1];
			String traceInfo = ste.getFileName();
			traceInfo += "@" + ste.getLineNumber() + ">>>";
			android.util.Log.w(tag, traceInfo + msgFormat);
		}
	}

	public static void w(String tag, String msgFormat, Throwable t) {
		if (PRINT_LOG && LEVEL <= android.util.Log.WARN) {
			android.util.Log.w(tag, msgFormat, t);
		}
	}

	// Error log
	public static void e(String tag, String msgFormat) {
		if (PRINT_LOG && LEVEL <= android.util.Log.ERROR) {
			StackTraceElement ste = new Throwable().getStackTrace()[1];
			String traceInfo = ste.getFileName();
			traceInfo += "@" + ste.getLineNumber() + ">>>";
			android.util.Log.e(tag, traceInfo + msgFormat);
		}
	}

	public static void e(String tag, String msgFormat, Throwable t) {
		if (PRINT_LOG && LEVEL <= android.util.Log.ERROR) {
			android.util.Log.d(tag, msgFormat, t);
		}
	}

	public static void v(String tag, String msgFormat, Object... args) {

		if (PRINT_LOG && LEVEL <= android.util.Log.VERBOSE) {
			android.util.Log.v(tag, String.format(msgFormat, args));
		}
	}

	public static void v(String tag, Throwable t, String msgFormat,
			Object... args) {
		if (PRINT_LOG && LEVEL <= android.util.Log.VERBOSE) {
			android.util.Log.v(tag, String.format(msgFormat, args), t);
		}
	}

	public static void d(String tag, String msgFormat, Object... args) {
		if (PRINT_LOG && LEVEL <= android.util.Log.DEBUG) {
			android.util.Log.d(tag, String.format(msgFormat, args));
		}
	}

	public static void d(String tag, Throwable t, String msgFormat,
			Object... args) {
		if (PRINT_LOG && LEVEL <= android.util.Log.DEBUG) {
			android.util.Log.d(tag, String.format(msgFormat, args), t);
		}
	}

	public static void i(String tag, String msgFormat, Object... args) {
		if (PRINT_LOG && LEVEL <= android.util.Log.INFO) {
			android.util.Log.i(tag, String.format(msgFormat, args));
		}
	}

	public static void i(String tag, Throwable t, String msgFormat,
			Object... args) {
		if (PRINT_LOG && LEVEL <= android.util.Log.INFO) {
			android.util.Log.i(tag, String.format(msgFormat, args), t);
		}
	}

	public static void w(String tag, String msgFormat, Object... args) {
		if (PRINT_LOG && LEVEL <= android.util.Log.WARN) {
			android.util.Log.w(tag, String.format(msgFormat, args));
		}
	}

	public static void w(String tag, Throwable t, String msgFormat,
			Object... args) {
		if (PRINT_LOG && LEVEL <= android.util.Log.WARN) {
			android.util.Log.w(tag, String.format(msgFormat, args), t);
		}
	}

	public static void e(String tag, String msgFormat, Object... args) {
		if (PRINT_LOG && LEVEL <= android.util.Log.ERROR) {
			android.util.Log.e(tag, String.format(msgFormat, args));
		}
	}

	public static void e(String tag, Throwable t, String msgFormat,
			Object... args) {
		if (PRINT_LOG && LEVEL <= android.util.Log.ERROR) {
			android.util.Log.e(tag, String.format(msgFormat, args), t);
		}
	}

}
