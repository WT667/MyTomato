package com.tomatocleaner.hook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 红果免费短剧 (com.phoenix.read) 界面精简 Hook。
 *
 * 清理内容：
 * <ul>
 *   <li>底部 tab：只保留 首页(VideoSeriesFeedTab) / 剧场(BookStore) / 我的(MyProfile)</li>
 *   <li>悬浮球：金币挂件、一键领挂件、主页面悬浮窗、增长悬浮条</li>
 * </ul>
 */
public class HongguoHook {

    private static final String TAG = "TomatoCleaner-Hongguo";
    private final XC_LoadPackage.LoadPackageParam lpparam;
    private final ClassLoader appCl;

    /** 红果底部 tab 白名单（注意：与番茄枚举含义不同） */
    private static final Set<String> BOTTOM_TABS_KEPT =
            new HashSet<>(Arrays.asList("VideoSeriesFeedTab", "BookStore", "MyProfile"));

    public HongguoHook(XC_LoadPackage.LoadPackageParam lpparam) {
        this.lpparam = lpparam;
        this.appCl = lpparam.classLoader;
    }

    public void start() {
        hookBottomTabs();
        hookFloatingBalls();
        XposedBridge.log(TAG + ": 所有 Hook 已挂载");
    }

    private void hookBottomTabs() {
        try {
            XposedHelpers.findAndHookMethod(
                    "com.dragon.read.pages.main.m", appCl,
                    "c", android.view.ViewGroup.class,
                    Class.forName("com.dragon.read.rpc.model.BottomTabBarItemType", false, appCl),
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object type = param.args[1];
                            if (type instanceof Enum) {
                                String name = ((Enum<?>) type).name();
                                if (!BOTTOM_TABS_KEPT.contains(name)) {
                                    XposedBridge.log(TAG + ": 屏蔽底部 tab: " + name);
                                    param.setResult(null);
                                }
                            }
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hookBottomTabs 跳过: " + t.getMessage());
        }
    }

    private void hookFloatingBalls() {
        // 金币挂件（KMP 版 + 老式版）
        hookReturnNull("com.bytedance.ug.sdk.kmp.novel.pendant.manager.PendantController", "s", "pendant-kmp");
        hookReturnNull("com.bytedance.ug.sdk.kmp.novel.pendant.manager.PendantController", "t", "pendant-kmp-alt");
        hookReturnNull("r32.o", "t", "pendant-legacy");

        // 主页面悬浮窗
        hookReturnNull("com.dragon.read.pages.main.z", "h", "main-window");

        // 增长悬浮条
        for (String m : new String[]{"m", "tryShowLowTakeCashFloatingView",
                "tryShowComic7DayFloatingView", "tryShowFestivalComicGuideFloatingView"}) {
            hookReturnNull("com.dragon.read.polaris.cold.start.l", m, "growth-" + m);
        }
    }

    private void hookReturnNull(String className, String method, String logTag) {
        try {
            XposedHelpers.findAndHookMethod(className, appCl, method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    XposedBridge.log(TAG + ": " + logTag);
                    param.setResult(null);
                }
            });
        } catch (Throwable ignored) {}
    }
}
