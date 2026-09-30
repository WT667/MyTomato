package com.tomatocleaner.hook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 模块入口：根据目标包名分发到对应 Hook 处理器。
 *
 * 支持三个目标 App：
 * <ul>
 *   <li>番茄畅听  — com.xs.fm      → {@link FanqieListenHook}</li>
 *   <li>番茄免费小说 — com.dragon.read → {@link FanqieNovelHook}</li>
 *   <li>红果免费短剧 — com.phoenix.read → {@link HongguoHook}</li>
 * </ul>
 */
public class XposedEntry implements IXposedHookLoadPackage {

    private static final String TAG = "TomatoCleaner";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        String pkg = lpparam.packageName;

        switch (pkg) {
            case "com.xs.fm":
                XposedBridge.log(TAG + ": 注入番茄畅听");
                new FanqieListenHook(lpparam).start();
                break;
            case "com.dragon.read":
                XposedBridge.log(TAG + ": 注入番茄免费小说");
                new FanqieNovelHook(lpparam).start();
                break;
            case "com.phoenix.read":
                XposedBridge.log(TAG + ": 注入红果免费短剧");
                new HongguoHook(lpparam).start();
                break;
            default:
                // 其他 App 不做任何事
                break;
        }
    }
}
