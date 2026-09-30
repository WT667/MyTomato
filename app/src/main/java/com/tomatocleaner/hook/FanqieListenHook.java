package com.tomatocleaner.hook;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 番茄畅听 (com.xs.fm) 净化 Hook。
 *
 * 移植自 fanqie-enhance v1.9.x，保留核心功能：
 * <ul>
 *   <li>VIP patch — 反射写 AcctUserModel.isVip/freeAd=true</li>
 *   <li>广告 Activity 拦截 — 启动即 finish</li>
 *   <li>TextView 广告文本过滤 — setText 时检测广告文案即隐藏</li>
 *   <li>广告位总闸 — hook AdConfigManager.checkAdAvailable 返回 false</li>
 * </ul>
 */
public class FanqieListenHook {

    private static final String TAG = "TomatoCleaner-FanqieListen";
    private final XC_LoadPackage.LoadPackageParam lpparam;
    private ClassLoader appCl;
    private int patchTries = 0;
    private boolean vipPatched = false;
    private Object patchedUserModel = null;

    /** 启动即拦截的广告/商城/任务 Activity 包名前缀 */
    private static final String[] BLOCKED_PREFIXES = {
            "com.bytedance.ug.sdk.luckycat.",
            "com.ss.android.excitingvideo.",
            "com.dragon.read.ad.",
            "com.dragon.read.admodule.",
            "com.dragon.read.pages.splash.ad.",
            "com.dragon.read.mall.",
            "com.dragon.read.task.",
            "com.dragon.read.coin.",
            "com.dragon.read.welfare.",
            "com.dragon.read.wallet.",
            "com.dragon.read.invite.",
            "com.dragon.read.redpacket.",
            "com.xs.fm.live.impl.ecom.",
    };

    /** 广告文本关键词（命中即隐藏 TextView） */
    private static final String[] AD_TEXTS = {
            "看小视频免", "看剧赚钱", "领金币", "领取金币", "金币待领",
            "立即领取", "听歌领", "签到", "看视频免广告",
    };

    public FanqieListenHook(XC_LoadPackage.LoadPackageParam lpparam) {
        this.lpparam = lpparam;
        this.appCl = lpparam.classLoader;
    }

    public void start() {
        hookActivityBlocker();
        hookTextViewAdFilter();
        hookAdConfigGate();
        scheduleVipPatch();
        XposedBridge.log(TAG + ": 所有 Hook 已挂载");
    }

    // ── VIP Patch ──────────────────────────────────────────────

    private void scheduleVipPatch() {
        new Handler(Looper.getMainLooper()).postDelayed(this::patchVip, 1500);
    }

    private void patchVip() {
        if (vipPatched && patchedUserModel != null) return;
        try {
            Class<?> acct = Class.forName("com.dragon.read.user.AcctManager", true, appCl);
            Field instF = acct.getDeclaredField("INSTANCE");
            instF.setAccessible(true);
            Object acctObj = instF.get(null);
            if (acctObj == null) { retry(); return; }

            Field umF = acct.getDeclaredField("userModel");
            umF.setAccessible(true);
            Object model = umF.get(acctObj);
            if (model == null) { retry(); return; }
            if (model == patchedUserModel) return;

            setField(model, "isVip", true);
            setField(model, "freeAd", true);
            trySetField(model, "expireTime", "6666-06-06");
            trySetField(model, "leftTime", "999999999");
            trySetField(model, "reverseVIP", true);
            trySetField(model, "freeAdLeft", 999999999L);
            trySetField(model, "freeAdExpire", 2147483647000L);

            patchedUserModel = model;
            vipPatched = true;
            XposedBridge.log(TAG + ": VIP patch 成功 isVip=true");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": patchVip 异常: " + t.getMessage());
            retry();
        }
    }

    private void retry() {
        if (patchTries++ < 8) {
            new Handler(Looper.getMainLooper()).postDelayed(this::patchVip, 800);
        }
    }

    private void setField(Object obj, String name, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private void trySetField(Object obj, String name, Object value) {
        try { setField(obj, name, value); } catch (Throwable ignored) {}
    }

    // ── Activity 拦截 ───────────────────────────────────────────

    private void hookActivityBlocker() {
        try {
            XposedHelpers.findAndHookMethod(
                    "android.app.Activity", appCl,
                    "onResume", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Activity act = (Activity) param.thisObject;
                            String cn = act.getClass().getName();
                            for (String prefix : BLOCKED_PREFIXES) {
                                if (cn.startsWith(prefix)) {
                                    XposedBridge.log(TAG + ": 拦截广告页 " + cn);
                                    act.finish();
                                    return;
                                }
                            }
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hookActivityBlocker 失败: " + t.getMessage());
        }
    }

    // ── TextView 广告文本过滤 ──────────────────────────────────

    private void hookTextViewAdFilter() {
        try {
            XposedHelpers.findAndHookMethod(TextView.class, "setText",
                    CharSequence.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            CharSequence cs = (CharSequence) param.args[0];
                            if (cs == null) return;
                            String t = cs.toString().trim();
                            if (t.isEmpty() || t.length() > 20) return;
                            for (String kw : AD_TEXTS) {
                                if (t.contains(kw)) {
                                    ((TextView) param.thisObject).setVisibility(View.GONE);
                                    return;
                                }
                            }
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hookTextViewAdFilter 失败: " + t.getMessage());
        }
    }

    // ── 广告位总闸 ─────────────────────────────────────────────

    private void hookAdConfigGate() {
        try {
            XposedHelpers.findAndHookMethod(
                    "com.dragon.read.base.ad.AdConfigManager", appCl,
                    "checkAdAvailable", String.class, String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String adSlot = (String) param.args[0];
                            // 用户主动触发的激励视频/金币流程放行
                            if (adSlot != null && (adSlot.contains("reward") || adSlot.contains("inspire"))) {
                                return;
                            }
                            param.setResult(false);
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hookAdConfigGate 跳过 (类不存在)");
        }
    }
}
