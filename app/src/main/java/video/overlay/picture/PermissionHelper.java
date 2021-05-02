package video.overlay.picture;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

public class PermissionHelper {

    private final static int PERMISSIONCODE = 100;
    public static boolean checkPermissions(Context context){
        boolean result =  ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        return result;
    }

    public static void requestPermissions(HomeFragment homeFragment){
        homeFragment.requestPermissions(
                new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONCODE);
    }

    public static boolean checkRequest(int requestCode,int[] grantResults){
        boolean result = false;
        result = requestCode == PERMISSIONCODE
                && grantResults.length == 2
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED;
        return result;
    }

}
