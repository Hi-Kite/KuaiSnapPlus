import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import dalvik.system.PathClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.io.File;
import java.lang.reflect.Method;

/**
 * 免重启调试入口
 * 1. 安装模块后只需重启一次目标 App（无需重启手机）
 * 2. 后续改代码→编译→安装，直接生效
 */
public class xposedmain implements IXposedHookLoadPackage {

    // ↓↓↓ 按实际改成你的 ↓↓↓
    private final String modulePkg   = "com.kite.kuaisnapplus";   // 模块包名
    private final String logicClass  = "xposed"; // 入口类（位于默认包，非 com.kite.kuaisnapplus）
    private final String logicMethod = "handleLoadPackage";     // 入口方法
    // ↑↑↑ 按实际改成你的 ↑↑↑

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context ctx = (Context) param.args[0];
                // 把宿主 ClassLoader 替换掉，防止多 dex 找不到类
                lpparam.classLoader = ctx.getClassLoader();

                // 调试阶段：动态加载 apk 里的最新代码
                File apk = findApk(ctx, modulePkg);
                if (apk == null) throw new RuntimeException("找不到模块 apk");

                PathClassLoader pcl = new PathClassLoader(apk.getAbsolutePath(),
                                                          ClassLoader.getSystemClassLoader());
                Class<?> clazz = Class.forName(logicClass, true, pcl);
                Object instance = clazz.newInstance();
                Method method = clazz.getDeclaredMethod(logicMethod, XC_LoadPackage.LoadPackageParam.class);
                method.invoke(instance, lpparam);
            }
        });
    }

    /* 根据包名定位本模块 apk */
    private File findApk(Context ctx, String pkg) {
        try {
            Context modCtx = ctx.createPackageContext(pkg,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            return new File(modCtx.getPackageCodePath());
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
