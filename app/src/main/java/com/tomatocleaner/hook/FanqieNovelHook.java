package com.tomatocleaner.hook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 番茄免费小说 (com.dragon.read) 界面精简 Hook。
 *
 * 移植自 ClearApp，核心策略：在宿主方法层面直接返回 null/false，
 * 让目标 View 和请求根本不产生，而不是事后隐藏。
 *
 * 清理内容：
 * <ul>
 *   <li>底部 tab：只保留 书城 / 书架 / 我的</li>
 *   <li>书城主 tab：去掉 看剧 / 视频 / 商城</li>
 *   <li>书架子 tab：去掉 收藏</li>
 *   <li>悬浮球：金币挂件、继续看悬浮窗、增长悬浮条、发帖球</li>
 *   <li>我的页：金币/提现卡片、推荐流</li>
 * </ul>
 */
public class FanqieNovelHook {

    private static final String TAG = "TomatoCleaner-FanqieNovel";
    private final XC_LoadPackage.LoadPackageParam lpparam;
    private final ClassLoader appCl;

    /** 底部 tab 白名单 */
    private static final Set<String> BOTTOM_TABS_KEPT =
            new HashSet<>(Arrays.asList("BookStore", "BookShelf", "MyProfile"));

    /** 书城主 tab 内要屏蔽的子 tab */
    private static final Set<String> BOOKSTORE_TABS_BLOCKED =
            new HashSet<>(Arrays.asList("看剧", "视频", "商城"));

    public FanqieNovelHook(XC_LoadPackage.LoadPackageParam lpparam) {
        this.lpparam = lpparam;
        this.appCl = lpparam.classLoader;
    }

    public void start() {
        hookBottomTabs();
        hookBookstoreSubTabs();
        hookFloatingBalls();
        hookProfilePage();
        XposedBridge.log(TAG + ": 所有 Hook 已挂载");
    }

    // ── 底部 tab：只保留白名单 ──────────────────────────────────

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

    // ── 书城主 tab 子 tab：过滤掉看剧/视频/商城 ──────────────────

    @SuppressWarnings("unchecked")
    private void hookBookstoreSubTabs() {
        try {
            XposedHelpers.findAndHookMethod(
                    "com.dragon.read.model.BookMallDefaultTabData", appCl,
                    "a", int.class, List.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            int selected = (int) param.args[0];
                            List<Object> tabs = (List<Object>) param.args[1];
                            if (tabs == null) return;

                            List<Object> kept = new java.util.ArrayList<>();
                            int newSelected = selected;
                            for (int i = 0; i < tabs.size(); i++) {
                                Object tab = tabs.get(i);
                                String tabName = readField(tab, "tabName");
                                if (tabName != null && BOOKSTORE_TABS_BLOCKED.contains(tabName)) {
                                    XposedBridge.log(TAG + ": 屏蔽子 tab: " + tabName);
                                    if (i == selected) newSelected = 0;
                                    else if (i < selected) newSelected--;
                                } else {
                                    kept.add(tab);
                                }
                            }
                            param.args[0] = newSelected;
                            param.args[1] = kept;
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hookBookstoreSubTabs 跳过: " + t.getMessage());
        }
    }

    // ── 悬浮球：金币挂件 / 继续看 / 增长条 / 发帖球 ──────────────

    private void hookFloatingBalls() {
        // 金币/福利挂件（UG SDK pendant）— attach 即返回 null
        tryHook("com.bytedance.ug.sdk.kmp.novel.pendant.manager.PendantController",
                "s", "float-pendant");
        tryHook("com.bytedance.ug.sdk.kmp.novel.pendant.manager.PendantController",
                "t", "float-pendant-alt");

        // 继续看悬浮球（KMP 版）
        tryHook("com.dragon.read.kmp.bookmall.floatview.FloatViewShowManager",
                "j", "float-continue-ball");

        // 主页面悬浮窗
        tryHook("com.dragon.read.pages.main.z", "h", "float-main-window");

        // 发帖球
        tryHook("ma5.o", "g", "float-editor-ball");

        // 冷启增长悬浮条
        for (String method : new String[]{"m", "tryShowLowTakeCashFloatingView",
                "tryShowComic7DayFloatingView", "tryShowFestivalComicGuideFloatingView"}) {
            tryHook("com.dragon.read.polaris.cold.start.l", method, "float-growth-" + method);
        }
    }

    private void tryHook(String className, String method, String logTag) {
        try {
            XposedHelpers.findAndHookMethod(className, appCl, method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    XposedBridge.log(TAG + ": " + logTag);
                    param.setResult(null);
                }
            });
        } catch (Throwable ignored) {
            // 类或方法不存在就跳过
        }
    }

    // ── 我的页：金币卡片 / 推荐流 ────────────────────────────────

    private void hookProfilePage() {
        // 关闭我的页推荐流
        tryHook("com.dragon.read.base.ssconfig.template.d", "d", "mine-recommend-feed");

        // 金币/提现 cell 入口
        try {
            XposedHelpers.findAndHookMethod(
                    "com.dragon.read.component.biz.impl.mine.card.IBsMineTabCellService$IMPL",
                    appCl, "isCellTypeShowable", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            int cell = (int) param.args[0];
                            // 2=金币余额, 119=番茄金币, 124=微信提现, 134=红果金币
                            if (cell == 2 || cell == 119 || cell == 124 || cell == 134) {
                                param.setResult(false);
                            }
                        }
                    });
        } catch (Throwable ignored) {}
    }

    // ── 工具 ───────────────────────────────────────────────────

    private String readField(Object obj, String fieldName) {
        try {
            Field f = findField(obj.getClass(), fieldName);
            if (f != null) {
                f.setAccessible(true);
                Object v = f.get(obj);
                return v != null ? v.toString() : null;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private Field findField(Class<?> cls, String name) {
        while (cls != null) {
            try { return cls.getDeclaredField(name); } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            }
        }
        return null;
    }
}
