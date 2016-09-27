package gmailfs.framework;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.GmailScopes;

import java.util.Arrays;

import pronus.gmailfs.MainActivity;

/**
 * Created by pronus on 8/25/16.
 */
public class AppContext {

    public static GoogleAccountCredential credential;
    public static FileSystem fs;
    public static String user;
    public static Integer screenWidth, screenHeight;

}
