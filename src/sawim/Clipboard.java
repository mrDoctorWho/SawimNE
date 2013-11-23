package sawim;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Build;
import ru.sawim.General;
import ru.sawim.activities.SawimActivity;

public final class Clipboard {

    private static void insertQuotingChars(StringBuffer out, CharSequence text, boolean isSubstring, char qChars) {
        int size = text.length();
        int quoteMaxLen = 100;
        boolean isSubstr = size > quoteMaxLen && isSubstring;
        if (isSubstr) size = quoteMaxLen;
        boolean wasNewLine = true;
        for (int i = 0; i < size; ++i) {
            char chr = text.charAt(i);
            if (wasNewLine) out.append(qChars).append(' ');
            out.append(chr);
            wasNewLine = (chr == '\n');
        }
        if (isSubstr) out.append("...");
    }

    public static String serialize(boolean isSubstring, boolean incoming, String header, CharSequence text) {
        StringBuffer sb = new StringBuffer();
        sb.append('[').append(header).append(']').append('\n');
        insertQuotingChars(sb, text, isSubstring, incoming ? '\u00bb' : '\u00ab');
        return sb.toString();
    }

    public static void setClipBoardText(String text) {
        putToClipboard(text);
    }

    public static void putToClipboard(final String text) {
        //SawimApplication.getSawimActivity().runOnUiThread(new Runnable() {
        //    public void run() {
        try {
            if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                android.text.ClipboardManager clipboard = (android.text.ClipboardManager)
                        General.currentActivity.getSystemService(SawimActivity.CLIPBOARD_SERVICE);
                clipboard.setText(text);
            } else {
                ClipboardManager clipboard = (ClipboardManager)
                        General.currentActivity.getSystemService(SawimActivity.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("", text));
            }
        } catch (Throwable e) {
            sawim.modules.DebugLog.panic("set clipboard", e);
        }
        //    }
        //});
    }
}