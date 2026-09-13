import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.hardware.*;
import android.media.*;
import android.os.*;
import android.text.*;
import android.util.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.*;
import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;

public class xposed implements IXposedHookLoadPackage {

	public static Activity Context;
	private static final String PREFS_NAME = "KuaiSnap_Settings";

	private static final String IMAGE_DIR = "/storage/emulated/0/Android/data/com.kuaiduizuoye.scan/files/image/";
	private static final String TARGET_SUFFIX = "_TRANSITION2.jpg";

	// 广告URL列表
	public static final String[] adStarts = {"https://adx.zuoyebang.com", "https://c.kuaiduizuoye.com/adx",
			"https://ad.", "https://ads.", "http://ad.", "http://ads."};

	// 作用域包名
	private static final String TARGET_PACKAGE = "com.kuaiduizuoye.scan";

	/*
	 * ============================ 版本适配 ============================
	 * 快对在版本更新后会重新混淆关键类名与方法名，部分资源 ID 也会整体偏移。
	 * 以下配置按版本列出所有 Hook 目标，运行时根据作用域软件的版本自动选择。
	 *
	 * 已适配版本：
	 *   6.77.0 (versionCode 1460)
	 *   7.7.0  (versionCode 1810)
	 *
	 * 新增版本适配步骤：
	 *   1. 用 jadx 反编译新版 APK，按“语义”而非名字定位下列各 Hook 点；
	 *   2. 用 apktool 反编译资源，确认各控件 ID（资源名已被混淆，需按结构比对）；
	 *   3. 复制一份 VerSpec 并填写，然后加入 selectSpec() 的识别逻辑。
	 * ==================================================================
	 */

	/** 单个快对版本的 Hook 目标配置 */
	static class VerSpec {
		/** 版本标识，仅用于日志 */
		String label;
		/** 会员失效弹窗判定：返回 true 时会弹出“会员已失效”，需强制为 false */
		String clsVipDialogShow;
		String mtdVipDialogShow;
		/** 会员状态（字符串 "1"/"0"） */
		String clsVipStateStr;
		String mtdVipStateStr;
		/** MineFragment 中的会员状态（字符串） */
		String mtdMineFragmentVipStr;
		/** activity.mine.util.c 中的会员状态（字符串） */
		String mtdMineUtilVipStr;
		/** 广告配置工具中的会员判定 */
		String clsFastAdConfig;
		String mtdFastAdVip;
		/** vip.status 原始数值（int） */
		String clsRawVipStatus;
		String mtdRawVipStatus;
		/** 布尔型会员判断（横屏浏览等 VIP 功能的开关） */
		String mtdVipGate;
		/** 高清特权 */
		String clsHdPrivilege;
		String mtdHdPrivilege;
		/** 设置 FLAG_SECURE 的方法（截屏限制） */
		String mtdScreenCapture;
		/** 退出解析页时弹出收藏弹窗的方法 */
		String mtdCollectDialog;
		/** 保存图片对话框的点击监听器：类名与方法名 */
		String clsSaveImageListener;
		String mtdSaveImageListener;
		/** “保存图片”按钮 ID */
		int idSaveImage;
		/** 主页红包推广 ID */
		int idRedPacket;
		/** 解析页“勤动脑多思考”横条 ID */
		int idNoticeBar;
		/** “横屏浏览”右上角 VIP 角标 ID */
		int idVipBadge;
		/** “我的”页面会员 Banner ID */
		int idVipBanner;
		/** 讲解视频遮挡 WebView ID */
		int idVideoMask;
		/** 网页版图片浏览页的水印合成方法（方法名为 null 表示该版本没有水印逻辑） */
		String mtdWatermark;
	}

	/** 双方版本一致的稳定类名 */
	private static final String CLS_MINE_FRAGMENT = "com.kuaiduizuoye.scan.activity.mine.fragment.MineFragment";
	private static final String CLS_MINE_UTIL = "com.kuaiduizuoye.scan.activity.mine.util.c";
	private static final String CLS_MINE_LOGIN_VIEW = "com.kuaiduizuoye.scan.activity.mine.widget.MineUserLoginView";
	private static final String CLS_MINE_AI_LOGIN_VIEW = "com.kuaiduizuoye.scan.activity.mine.widget.MineAiUserLoginView";
	private static final String CLS_MAIN_ACTIVITY = "com.kuaiduizuoye.scan.activity.main.activity.MainActivity";
	private static final String CLS_BOOK_BROWSE = "com.kuaiduizuoye.scan.activity.scan.activity.BookCompleteDetailsPictureBrowseActivity";
	private static final String CLS_SEARCH_SCAN = "com.kuaiduizuoye.scan.activity.scan.activity.SearchScanCodeResultActivity";
	private static final String CLS_VIDEO_ACTIVITY = "com.kuaiduizuoye.scan.activity.video.sdk.VideoPlayerActivity";
	private static final String CLS_MULTI_VIDEO_BEAN = "com.zybang.sdk.player.ui.model.MultipleVideoBean";
	private static final String CLS_WEB_PICTURE_BROWSE = "com.kuaiduizuoye.scan.activity.common.CommonWebPictureBrowseActivity";

	/** 快对 6.77.0 的适配配置 */
	private static final VerSpec V6_77_0 = new VerSpec();
	/** 快对 7.7.0 的适配配置 */
	private static final VerSpec V7_7_0 = new VerSpec();

	static {
		// ------------------------------ 6.77.0 ------------------------------
		V6_77_0.label = "6.77.0";
		V6_77_0.clsVipDialogShow = "com.kuaiduizuoye.scan.activity.database.a.i";
		V6_77_0.mtdVipDialogShow = "a";
		V6_77_0.clsVipStateStr = "com.kuaiduizuoye.scan.activity.database.a.i";
		V6_77_0.mtdVipStateStr = "c";
		V6_77_0.mtdMineFragmentVipStr = "h";
		V6_77_0.mtdMineUtilVipStr = "k";
		V6_77_0.clsFastAdConfig = "com.kuaiduizuoye.scan.activity.newadvertisement.f.a";
		V6_77_0.mtdFastAdVip = "c";
		V6_77_0.clsRawVipStatus = "com.kuaiduizuoye.scan.activity.vip.a.a";
		V6_77_0.mtdRawVipStatus = "c";
		V6_77_0.mtdVipGate = "a";
		V6_77_0.clsHdPrivilege = "com.kuaiduizuoye.scan.activity.scan.util.ai";
		V6_77_0.mtdHdPrivilege = "b";
		V6_77_0.mtdScreenCapture = "u";
		V6_77_0.mtdCollectDialog = "M";
		V6_77_0.clsSaveImageListener = "com.kuaiduizuoye.scan.activity.scan.util.-$$Lambda$az$yoGpX1V-cpnva81wmw-_K8CTTPI";
		V6_77_0.mtdSaveImageListener = "onClick";
		V6_77_0.idSaveImage = 0x7f090e80;
		V6_77_0.idRedPacket = 0x7f090c82;
		V6_77_0.idNoticeBar = 0x7f090144;
		V6_77_0.idVipBadge = 0x7f0910e7;
		V6_77_0.idVipBanner = 0x7f091224;
		V6_77_0.idVideoMask = 0x7f0912e3;
		// 6.77.0 的图片浏览页没有水印逻辑，无需处理
		V6_77_0.mtdWatermark = null;

		// ------------------------------ 7.7.0 ------------------------------
		// 关键类被重新混淆：database.a.i -> dh.i；newadvertisement.f.a -> aj.a
		//                   vip.a.a -> bk.a；scan.util.ai -> oj.p0
		//                   scan.util.az -> oj.p1（其匿名监听器 -> oj.o1）
		V7_7_0.label = "7.7.0";
		V7_7_0.clsVipDialogShow = "dh.i";
		V7_7_0.mtdVipDialogShow = "j";
		V7_7_0.clsVipStateStr = "dh.i";
		V7_7_0.mtdVipStateStr = "d";
		V7_7_0.mtdMineFragmentVipStr = "d0";
		V7_7_0.mtdMineUtilVipStr = "i";
		V7_7_0.clsFastAdConfig = "aj.a";
		V7_7_0.mtdFastAdVip = "j";
		V7_7_0.clsRawVipStatus = "bk.a";
		V7_7_0.mtdRawVipStatus = "f";
		V7_7_0.mtdVipGate = "o";
		V7_7_0.clsHdPrivilege = "oj.p0";
		V7_7_0.mtdHdPrivilege = "a";
		V7_7_0.mtdScreenCapture = "W3";
		V7_7_0.mtdCollectDialog = "S2";
		V7_7_0.clsSaveImageListener = "oj.o1";
		V7_7_0.mtdSaveImageListener = "onClick";
		V7_7_0.idSaveImage = 0x7f090eba;
		V7_7_0.idRedPacket = 0x7f090cc7;
		V7_7_0.idNoticeBar = 0x7f09013f;
		V7_7_0.idVipBadge = 0x7f0910f7;
		V7_7_0.idVipBanner = 0x7f091229;
		V7_7_0.idVideoMask = 0x7f0912e4;
		// 7.7.0 新增：网页版图片浏览页会把本地水印图平铺合成到显示用位图上
		V7_7_0.mtdWatermark = "c2";
	}

	/** 当前生效的版本配置，默认按最新版处理 */
	private static VerSpec spec = V7_7_0;

	/**
	 * 选择适配配置：优先按版本名判断，失败时按关键类是否存在来探测。
	 */
	private static VerSpec selectSpec(Context context, ClassLoader classLoader) {
		String versionName = null;
		int versionCode = -1;
		try {
			android.content.pm.PackageInfo info = context.getPackageManager().getPackageInfo(TARGET_PACKAGE, 0);
			versionName = info.versionName;
			versionCode = info.versionCode;
		} catch (Throwable t) {
			XposedBridge.log("读取快对版本信息失败: " + t);
		}

		if (versionName != null) {
			if (versionName.startsWith("7.")) {
				return V7_7_0;
			}
			if (versionName.startsWith("6.")) {
				return V6_77_0;
			}
		}

		// 版本名不可用时，按特征类探测：7.7.0 使用 oj.p1，6.77.0 使用 scan.util.az
		try {
			classLoader.loadClass(V7_7_0.clsSaveImageListener);
			return V7_7_0;
		} catch (Throwable ignored) {
		}
		try {
			classLoader.loadClass("com.kuaiduizuoye.scan.activity.scan.util.az");
			return V6_77_0;
		} catch (Throwable ignored) {
		}

		XposedBridge.log("无法识别快对版本(versionName=" + versionName + ", versionCode=" + versionCode
				+ ")，按 7.7.0 处理");
		return V7_7_0;
	}

	@Override
	public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam packageName) throws Throwable {

		// 通过自己Hook自己进行激活校验
		if (packageName.packageName.equals("com.kite.kuaisnapplus")) {
			try {
				Class<?> clazz = packageName.classLoader.loadClass("com.kite.kuaisnapplus.ModuleStatus"); // 加载校验类
				java.lang.reflect.Field field = clazz.getDeclaredField("activated");
				field.setAccessible(true);
				field.setBoolean(null, true);
			} catch (Throwable ignored) {
			}
			return;
		}

		if (!packageName.packageName.equals(TARGET_PACKAGE))
			return;

		XposedHelpers.findAndHookMethod(android.app.Application.class, "attach", Context.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				final Context context = (Context) param.args[0];
				final ClassLoader classLoader = context.getClassLoader();

				if (Context == null) {
					XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam parameter) throws Throwable {
							super.afterHookedMethod(parameter);
							if (Context == null) {
								Context = (Activity) parameter.thisObject;
								jiguromessage("領域展開，りょういきてんかい !");
								XposedBridge.log("得到ActivityContext");
							}
						}
					});
				}

				// 识别快对版本，选择对应的 Hook 目标
				spec = selectSpec(context, classLoader);
				XposedBridge.log("快怼+: 已识别快对版本 " + spec.label);

				// 设置所有hook
				setupAllHooks(context, classLoader);
			}
		});
	}

	private void setupAllHooks(Context context, ClassLoader classLoader) {
		try {
			// 基础会员解锁相关hook
			if (getBooleanSetting(context, "enable_vip", true)) {
				setupVipHooks(classLoader);
			}

			// 解锁横屏旋转
			if (getBooleanSetting(context, "enable_rotate", true)) {
				setupRotateHooks(classLoader);
			}

			// 解锁高清内容
			if (getBooleanSetting(context, "enable_hd", true)) {
				setupHdHooks(classLoader);
			}

			// 会员金标
			if (getBooleanSetting(context, "enable_vip_badge", true)) {
				setupVipBadgeHooks(classLoader);
			}

			// 去除截屏限制
			if (getBooleanSetting(context, "enable_screen_capture", true)) {
				setupScreenCaptureHooks(classLoader);
			}

			// 去除广告
			if (getBooleanSetting(context, "enable_ad_block", true)) {
				setupAdBlockHooks(classLoader);
			}

			// 禁用传感器
			if (getBooleanSetting(context, "enable_sensor_block", false)) {
				setupSensorBlockHooks(classLoader);
			}

			// 屏蔽收藏弹窗
			if (getBooleanSetting(context, "block_collection_dialog", false)) {
				setupCollectionDialogHooks(classLoader);
			}

			// 图片解密
			if (getBooleanSetting(context, "enable_image_decrypt", true)) {
				setupImageDecryptHooks(classLoader);
			}

			// 去除新人会员banner
			if (getBooleanSetting(context, "block_new_user_banner", false)) {
				setupNewUserBannerHooks(classLoader);
			}

			// 去除会员Banner
			if (getBooleanSetting(context, "remove_vip_banner", false)) {
				setupVipBannerHooks(classLoader);
			}

			// 解锁讲解视频
			if (getBooleanSetting(context, "enable_video_explanation", false)) {
				setupVideoExplanationHooks(classLoader);
			}

			// 无水印查看（仅影响网页版图片浏览页的显示，不影响保存的图片）
			if (getBooleanSetting(context, "remove_watermark", true)) {
				setupWatermarkHooks(classLoader);
			}

			// 组件屏蔽相关hook
			setupViewBlockHooks(context, classLoader);
		} catch (Exception e) {
			XposedBridge.log("设置Hook时发生异常: " + e.getMessage());
		}
	}

	// 组件屏蔽hook
	private void setupViewBlockHooks(Context context, ClassLoader classLoader) {
		try {
			XposedBridge.log("开始设置组件屏蔽Hook");

			// 屏蔽主页面红包推广
			if (getBooleanSetting(context, "block_red_packet", false)) {
				XposedHelpers.findAndHookMethod(CLS_MAIN_ACTIVITY,
						classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
							@Override
							protected void afterHookedMethod(MethodHookParam param) throws Throwable {
								final Activity activity = (Activity) param.thisObject;
								new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
									@Override
									public void run() {
										try {
											View redPacketView = activity.findViewById(spec.idRedPacket);
											if (redPacketView == null) {
												XposedBridge.log("红包视图未找到");
											} else {
												ViewGroup parent = (ViewGroup) redPacketView.getParent();
												if (parent == null) {
													XposedBridge.log("红包视图的父视图为空");
												} else {
													parent.removeView(redPacketView);
													XposedBridge.log("已屏蔽红包推广组件");
												}
											}

										} catch (Exception e) {
											XposedBridge.log("屏蔽红包推广失败: " + e.getMessage());
										}
									}
								}, 1000);
							}
						});
			}

			// 屏蔽解析页面提示横条和 VIP角标
			XposedHelpers.findAndHookMethod(
					CLS_BOOK_BROWSE,
					classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable {
							final Activity activity = (Activity) param.thisObject;

							// 使用ViewTreeObserver监听视图变化，以便更快检测到横条
							final View decorView = activity.getWindow().getDecorView();
							decorView.getViewTreeObserver()
									.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
										@Override
										public void onGlobalLayout() {
											try {
												// 移除监听，避免重复执行
												decorView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

												// 屏蔽"勤动脑多思考"提示横条（基于文本关键字）
												if (getBooleanSetting(activity, "block_notice_bar", false)) {
													// 查找包含"勤动脑多思考"文本的视图
													View noticeTextView = findViewByText(decorView, "勤动脑多思考");
													if (noticeTextView != null) {
														// 找到文本视图后，向上查找整个横条的根布局
														ViewGroup noticeBar = findNoticeBarRoot(noticeTextView);
														if (noticeBar != null) {
															ViewGroup parent = (ViewGroup) noticeBar.getParent();
															if (parent != null) {
																parent.removeView(noticeBar);
																XposedBridge.log("已通过文本关键字屏蔽整个通知横条组件");
															}
														} else {
															// 如果找不到根布局，尝试移除文本视图的直接父布局
															ViewGroup parent = (ViewGroup) noticeTextView.getParent();
															if (parent != null) {
																parent.removeView(noticeTextView);
																XposedBridge.log("已通过文本关键字屏蔽通知文本组件");
															}
														}
													} else {
														// 如果基于文本查找失败，尝试使用原来的ID查找作为备用方案
														View noticeBarById = activity.findViewById(spec.idNoticeBar);
														if (noticeBarById != null) {
															ViewGroup parent = (ViewGroup) noticeBarById.getParent();
															if (parent != null) {
																parent.removeView(noticeBarById);
																XposedBridge.log("已通过ID屏蔽通知横条组件");
															}
														}
													}
												}

												// 屏蔽VIP相关控件（包括角标和文字）
												if (getBooleanSetting(activity, "block_vip_badge", true)) {
													// 屏蔽VIP角标（通过ID查找）
													View vipBadge = activity.findViewById(spec.idVipBadge);
													if (vipBadge != null) {
														ViewGroup parent = (ViewGroup) vipBadge.getParent();
														if (parent != null) {
															parent.removeView(vipBadge);
															XposedBridge.log("已屏蔽VIP角标组件");
														}
													}

													// 屏蔽所有文字包含"VIP"的控件
													removeViewsByText(decorView, "VIP");
												}
											} catch (Exception e) {
												XposedBridge.log("屏蔽解析页面组件失败: " + e.getMessage());
											}
										}
									});
						}
					});

			XposedBridge.log("组件屏蔽Hook设置成功");
		} catch (Exception e) {
			XposedBridge.log("组件屏蔽Hook设置失败: " + e.getMessage());
		}
	}

	// 通过文本内容查找视图
	private View findViewByText(View rootView, String targetText) {
		try {
			if (rootView instanceof ViewGroup) {
				ViewGroup group = (ViewGroup) rootView;
				for (int i = 0; i < group.getChildCount(); i++) {
					View child = group.getChildAt(i);
					View foundView = findViewByText(child, targetText);
					if (foundView != null) {
						return foundView;
					}
				}
			} else if (rootView instanceof TextView) {
				TextView textView = (TextView) rootView;
				String text = textView.getText().toString().replace(" ", "").replace("，", "");
				if (text.contains(targetText)) {
					return rootView;
				}
			}
		} catch (Exception e) {
			XposedBridge.log("通过文本查找视图时出错: " + e.getMessage());
		}
		return null;
	}

	// 移除所有包含特定文本的视图
	private void removeViewsByText(View rootView, String targetText) {
		try {
			if (rootView instanceof ViewGroup) {
				ViewGroup group = (ViewGroup) rootView;
				// 先处理子视图，然后处理当前组
				for (int i = 0; i < group.getChildCount(); i++) {
					View child = group.getChildAt(i);
					removeViewsByText(child, targetText);
				}
			}

			// 检查当前视图是否包含目标文本
			if (rootView instanceof TextView) {
				TextView textView = (TextView) rootView;
				String text = textView.getText().toString();
				if (text != null && text.contains(targetText)) {
					ViewGroup parent = (ViewGroup) rootView.getParent();
					if (parent != null) {
						parent.removeView(rootView);
						XposedBridge.log("已屏蔽包含'" + targetText + "'文本的组件: " + text);
					}
				}
			}
		} catch (Exception e) {
			XposedBridge.log("移除包含文本'" + targetText + "'的视图时出错: " + e.getMessage());
		}
	}

	// 查找通知横条的根布局
	private ViewGroup findNoticeBarRoot(View textView) {
		try {
			// 向上遍历父视图，寻找可能的横条根布局
			View current = textView;
			while (current != null && current.getParent() instanceof View) {
				View parent = (View) current.getParent();

				// 检查父视图是否可能是横条容器（根据常见的布局特征）
				if (parent instanceof ViewGroup) {
					ViewGroup parentGroup = (ViewGroup) parent;

					// 检查布局特征：宽度通常为MATCH_PARENT，高度较小，可能有特定背景色
					ViewGroup.LayoutParams params = parentGroup.getLayoutParams();
					if (params != null && (params.width == ViewGroup.LayoutParams.MATCH_PARENT
							|| (params.width > 0 && params.width < 1000)) && // 宽度较大但不是全屏
							parentGroup.getHeight() > 0 && parentGroup.getHeight() < 100) { // 高度较小
						return parentGroup;
					}

					// 检查是否有其他子视图（如关闭按钮）
					if (parentGroup.getChildCount() > 1) {
						boolean hasCloseButton = false;
						for (int i = 0; i < parentGroup.getChildCount(); i++) {
							View child = parentGroup.getChildAt(i);
							if (child != textView && (child instanceof ImageView || (child instanceof TextView
									&& ((TextView) child).getText().toString().contains("关闭")))) {
								hasCloseButton = true;
								break;
							}
						}
						if (hasCloseButton) {
							return parentGroup;
						}
					}
				}

				current = parent;
			}
		} catch (Exception e) {
			XposedBridge.log("查找横条根布局时出错: " + e.getMessage());
		}
		return null;
	}

	// 修改setupCollectionDialogHooks方法
	private void setupCollectionDialogHooks(ClassLoader classLoader) {
		try {
			XposedBridge.log("开始设置收藏弹窗屏蔽Hook");

			// Hook BookCompleteDetailsPictureBrowseActivity的返回键处理
			XposedHelpers.findAndHookMethod(
					CLS_BOOK_BROWSE,
					classLoader, "onBackPressed", new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
							try {
								Activity activity = (Activity) param.thisObject;
								XposedBridge.log("拦截BookCompleteDetailsPictureBrowseActivity弹窗，直接退出");
								activity.finish();
								param.setResult(null); // 阻止原方法执行
							} catch (Exception e) {
								XposedBridge.log("拦截BookCompleteDetailsPictureBrowseActivity弹窗失败: " + e.getMessage());
							}
						}
					});

			// Hook SearchScanCodeResultActivity的弹窗显示方法，然后立即退出Activity
			XposedHelpers.findAndHookMethod(CLS_SEARCH_SCAN,
					classLoader, spec.mtdCollectDialog, new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
							try {
								Activity activity = (Activity) param.thisObject;
								XposedBridge.log("拦截SearchScanCodeResultActivity弹窗，直接退出");

								// 立即退出Activity
								activity.finish();
								param.setResult(null); // 阻止原方法执行
							} catch (Exception e) {
								XposedBridge.log("拦截SearchScanCodeResultActivity弹窗失败: " + e.getMessage());
							}
						}
					});

			XposedBridge.log("收藏弹窗屏蔽Hook设置成功");
		} catch (Exception e) {
			XposedBridge.log("收藏弹窗屏蔽Hook设置失败: " + e.getMessage());
		}
	}

	// 从View中提取文本内容
	private String getTextFromView(View view) {
		try {
			if (view instanceof ViewGroup) {
				ViewGroup group = (ViewGroup) view;
				for (int i = 0; i < group.getChildCount(); i++) {
					String text = getTextFromView(group.getChildAt(i));
					if (text != null && !text.isEmpty()) {
						return text;
					}
				}
			} else if (view instanceof TextView) {
				return ((TextView) view).getText().toString();
			}
		} catch (Exception e) {
			XposedBridge.log("提取View文本时出错: " + e.getMessage());
		}
		return null;
	}

	// 广告拦截hook
	private void setupAdBlockHooks(ClassLoader classLoader) {
		try {
			XposedBridge.log("开始设置广告拦截Hook");

			// Hook URL构造函数来拦截广告请求
			XposedHelpers.findAndHookConstructor("java.net.URL", classLoader, String.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					String url = (String) param.args[0];
					if (url != null) {
						for (String adUrl : adStarts) {
							if (url.startsWith(adUrl)) {
								XposedBridge.log("拦截广告URL: " + url);
								// 将广告URL替换为空白页或无效URL
								param.args[0] = "about:blank";
								break;
							}
						}
					}
				}
			});

			// 拦截WebView中的广告加载
			XposedHelpers.findAndHookMethod("android.webkit.WebView", classLoader, "loadUrl", String.class,
					new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
							String url = (String) param.args[0];
							if (url != null) {
								for (String adUrl : adStarts) {
									if (url.startsWith(adUrl)) {
										XposedBridge.log("拦截WebView广告加载: " + url);
										param.setResult(null); // 阻止加载
										return;
									}
								}
							}
						}
					});

			XposedBridge.log("广告拦截Hook设置成功");
		} catch (Exception e) {
			XposedBridge.log("广告拦截Hook设置失败: " + e.getMessage());
		}
	}

	// 传感器禁用hook
	private void setupSensorBlockHooks(ClassLoader classLoader) {
		try {
			XposedBridge.log("开始设置传感器禁用Hook");

			// Hook SensorManager的getDefaultSensor方法
			XposedHelpers.findAndHookMethod("android.hardware.SensorManager", classLoader, "getDefaultSensor",
					int.class, new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable {
							int type = (Integer) param.args[0];
							// 禁用陀螺仪和加速度传感器
							if (type == Sensor.TYPE_GYROSCOPE || type == Sensor.TYPE_ACCELEROMETER) {
								XposedBridge.log("禁用传感器类型: " + type);
								param.setResult(null); // 返回null表示设备不支持该传感器
							}
						}
					});

			// Hook SensorManager的getSensorList方法
			XposedHelpers.findAndHookMethod("android.hardware.SensorManager", classLoader, "getSensorList", int.class,
					new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable {
							int type = (Integer) param.args[0];
							if (type == Sensor.TYPE_ALL || type == Sensor.TYPE_GYROSCOPE
									|| type == Sensor.TYPE_ACCELEROMETER) {
								// 返回空列表或过滤掉陀螺仪和加速度传感器
								java.util.List<Sensor> sensors = (java.util.List<Sensor>) param.getResult();
								if (sensors != null) {
									java.util.List<Sensor> filteredSensors = new java.util.ArrayList<>();
									for (Sensor sensor : sensors) {
										int sensorType = sensor.getType();
										if (sensorType != Sensor.TYPE_GYROSCOPE
												&& sensorType != Sensor.TYPE_ACCELEROMETER) {
											filteredSensors.add(sensor);
										} else {
											XposedBridge.log("过滤掉传感器: " + sensorType);
										}
									}
									param.setResult(filteredSensors);
								}
							}
						}
					});

			// Hook SensorManager的registerListener方法，防止注册陀螺仪和加速度传感器监听
			XposedHelpers.findAndHookMethod("android.hardware.SensorManager", classLoader, "registerListener",
					"android.hardware.SensorEventListener", Sensor.class, int.class, new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
							Sensor sensor = (Sensor) param.args[1];
							if (sensor != null) {
								int type = sensor.getType();
								if (type == Sensor.TYPE_GYROSCOPE || type == Sensor.TYPE_ACCELEROMETER) {
									XposedBridge.log("阻止注册传感器监听: " + type);
									param.setResult(false); // 注册失败
								}
							}
						}
					});

			XposedBridge.log("传感器禁用Hook设置成功");
		} catch (Exception e) {
			XposedBridge.log("传感器禁用Hook设置失败: " + e.getMessage());
		}
	}

	// 新人banner请求路径（快对通过 NetConfig.getHost() 拼接，故只能匹配路径部分）
	private static final String NEW_USER_BANNER_PATH = "/kdapi/conf/mycard";

	// 屏蔽新人banner的hook
	private void setupNewUserBannerHooks(final ClassLoader classLoader) {
		try {
			XposedBridge.log("开始设置新人banner屏蔽Hook");

			// Hook URL构造函数来拦截特定请求
			XposedHelpers.findAndHookConstructor("java.net.URL", classLoader, String.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					String url = (String) param.args[0];
					if (url != null && url.contains(NEW_USER_BANNER_PATH)) {
						XposedBridge.log("拦截新人banner请求: " + url);
						// 将URL替换为无效URL
						param.args[0] = "about:blank";
					}
				}
			});

			XposedBridge.log("新人banner屏蔽Hook设置成功");
		} catch (Exception e) {
			XposedBridge.log("新人banner屏蔽Hook设置失败: " + e.getMessage());
		}
	}

	// 去除会员Banner的hook
	private void setupVipBannerHooks(ClassLoader classLoader) {
		try {
			XposedBridge.log("开始设置会员Banner屏蔽Hook");

			// Hook MainActivity的onCreate方法
			XposedHelpers.findAndHookMethod(CLS_MAIN_ACTIVITY, classLoader,
					"onCreate", Bundle.class, new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable {
							final Activity activity = (Activity) param.thisObject;

							// 延迟执行以确保视图已加载
							new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
								@Override
								public void run() {
									try {
										if (activity.isFinishing() || activity.isDestroyed()) {
											return;
										}

										// 查找并移除会员Banner控件
										View vipBanner = activity.findViewById(spec.idVipBanner);
										if (vipBanner != null) {
											ViewGroup parent = (ViewGroup) vipBanner.getParent();
											if (parent != null) {
												parent.removeView(vipBanner);
												XposedBridge.log("成功屏蔽会员Banner (ID: " + Integer.toHexString(spec.idVipBanner) + ")");
											} else {
												XposedBridge.log("会员Banner的父视图为空");
											}
										} else {
											XposedBridge.log("未找到会员Banner (ID: " + Integer.toHexString(spec.idVipBanner) + ")");

											// 尝试通过其他方式查找
											tryFindVipBannerByOtherMeans(activity);
										}
									} catch (Exception e) {
										XposedBridge.log("屏蔽会员Banner失败: " + e.getMessage());
									}
								}
							}, 1000); // 延迟1秒执行
						}
					});

			XposedBridge.log("会员Banner屏蔽Hook设置成功");
		} catch (Exception e) {
			XposedBridge.log("会员Banner屏蔽Hook设置失败: " + e.getMessage());
		}
	}

	// 通过其他方式查找会员Banner
	private void tryFindVipBannerByOtherMeans(Activity activity) {
		try {
			// 通过文本内容查找
			View bannerByText = findViewByText(activity.getWindow().getDecorView(), "会员");
			if (bannerByText != null) {
				ViewGroup parent = (ViewGroup) bannerByText.getParent();
				if (parent != null) {
					parent.removeView(bannerByText);
					XposedBridge.log("通过文本内容成功屏蔽会员Banner");
					return;
				}
			}

			// 通过视图结构查找
			View bannerByStructure = findVipBannerByStructure(activity.getWindow().getDecorView());
			if (bannerByStructure != null) {
				ViewGroup parent = (ViewGroup) bannerByStructure.getParent();
				if (parent != null) {
					parent.removeView(bannerByStructure);
					XposedBridge.log("通过视图结构成功屏蔽会员Banner");
					return;
				}
			}

			XposedBridge.log("备用方案也未找到会员Banner");
		} catch (Exception e) {
			XposedBridge.log("备用方案查找会员Banner失败: " + e.getMessage());
		}
	}

	// 通过视图结构查找会员Banner
	private View findVipBannerByStructure(View rootView) {
		try {
			if (rootView instanceof ViewGroup) {
				ViewGroup group = (ViewGroup) rootView;
				for (int i = 0; i < group.getChildCount(); i++) {
					View child = group.getChildAt(i);

					// 检查视图特征，通常会员Banner会有特定的大小和位置
					if (child.getHeight() > 0 && child.getHeight() < 200 && child.getWidth() > 0
							&& child.getWidth() > group.getWidth() / 2) {
						// 可能是横幅广告
						return child;
					}

					// 递归查找子视图
					View found = findVipBannerByStructure(child);
					if (found != null) {
						return found;
					}
				}
			}
		} catch (Exception e) {
			XposedBridge.log("通过结构查找会员Banner失败: " + e.getMessage());
		}
		return null;
	}

	// 添加图片解密hook方法
	private void setupImageDecryptHooks(ClassLoader classLoader) {
		try {
			XposedBridge.log("开始设置图片解密Hook");

			// Hook 保存图片对话框的点击监听器
			XposedHelpers.findAndHookMethod(
					spec.clsSaveImageListener, classLoader,
					spec.mtdSaveImageListener, View.class, new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
							// 获取被点击的View
							View view = (View) param.args[0];
							int viewId = view.getId();

							XposedBridge.log("点击的按钮ID: " + Integer.toHexString(viewId));

							// 只对“保存图片”按钮进行处理
							if (viewId == spec.idSaveImage) {
								// 阻止原方法执行
								param.setResult(null);

								// 获取Context
								final Context context = view.getContext();

								if (context != null) {
									// 方法1：通过反射获取并关闭对话框
									try {
										// 获取lambda实例中的f$1字段（对话框实例）
										Object lambdaInstance = param.thisObject;
										Object dialogInstance = XposedHelpers.getObjectField(lambdaInstance, "f$1");

										if (dialogInstance != null) {
											// 调用dismissViewDialog方法
											XposedHelpers.callMethod(dialogInstance, "dismissViewDialog");
											XposedBridge.log("成功关闭应用原对话框");
										}
									} catch (Throwable e) {
										XposedBridge.log("关闭对话框失败: " + e.getMessage());
									}

									// 在主线程显示加载对话框
									new Handler(Looper.getMainLooper()).post(new Runnable() {
										@Override
										public void run() {
											try {
												showDecryptProgressDialog(context);

												// 在后台线程执行解密操作
												new Thread(new Runnable() {
													@Override
													public void run() {
														// 记录开始时间
														final long startTime = System.currentTimeMillis();

														// 使用final数组来存储结果
														final boolean[] success = {false};
														final String[] errorMessage = {null};
														final List<FileInfo>[] decryptedFiles = new List[1];

														try {
															// 执行解密操作
															decryptedFiles[0] = decryptTransitionImages();
															success[0] = true;
															XposedBridge.log(
																	"图片解密完成，共解密: " + decryptedFiles[0].size() + "张图片");
														} catch (Exception e) {
															XposedBridge.log("图片解密失败: " + e.getMessage());
															errorMessage[0] = e.getMessage();
														}

														// 计算需要等待的时间（至少3秒）
														final long elapsedTime = System.currentTimeMillis() - startTime;
														final long remainingTime = Math.max(3000 - elapsedTime, 0);

														// 等待剩余时间
														if (remainingTime > 0) {
															try {
																Thread.sleep(remainingTime);
															} catch (InterruptedException e) {
																XposedBridge.log("等待线程被中断: " + e.getMessage());
															}
														}

														// 在主线程更新UI
														new Handler(Looper.getMainLooper()).post(new Runnable() {
															@Override
															public void run() {
																try {
																	hideDecryptProgressDialog();
																	if (success[0]) {
																		if (decryptedFiles[0].size() > 0) {
																			// 显示图片选择对话框
																			showImageSelectionDialog(context,
																					decryptedFiles[0]);
																		} else {
																			Toast.makeText(context, "未找到可解密的图片",
																					Toast.LENGTH_SHORT).show();
																		}
																	} else {
																		Toast.makeText(context,
																				"图片解密失败: " + errorMessage[0],
																				Toast.LENGTH_SHORT).show();
																	}
																} catch (Exception e) {
																	XposedBridge.log("UI更新失败: " + e.getMessage());
																}
															}
														});
													}
												}).start();
											} catch (Exception e) {
												XposedBridge.log("显示对话框失败: " + e.getMessage());
											}
										}
									});
								}
							}
						}
					});


			XposedBridge.log("图片解密Hook设置成功");
		} catch (Exception e) {
			XposedBridge.log("图片解密Hook设置失败: " + e.getMessage());
		}
	}

	// 成员变量声明
	private Dialog decryptProgressDialog;

	private void showDecryptProgressDialog(final Context context) {
		try {
			if (decryptProgressDialog != null && decryptProgressDialog.isShowing()) {
				decryptProgressDialog.dismiss();
			}

			// 创建自定义样式的对话框
			decryptProgressDialog = new Dialog(context);
			decryptProgressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			decryptProgressDialog.setCancelable(false);

			// 设置对话框窗口属性
			Window window = decryptProgressDialog.getWindow();
			if (window != null) {
				window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
				window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			}

			// 创建主容器
			LinearLayout mainLayout = new LinearLayout(context);
			mainLayout.setOrientation(LinearLayout.VERTICAL);
			mainLayout.setGravity(Gravity.CENTER);
			mainLayout.setPadding(dp(context, 24), dp(context, 24), dp(context, 24), dp(context, 24));

			// 设置白色背景和圆角
			GradientDrawable bg = new GradientDrawable();
			bg.setColor(Color.WHITE);
			bg.setCornerRadius(dp(context, 12));
			mainLayout.setBackground(bg);

			// 添加阴影
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				mainLayout.setElevation(dp(context, 8));
			}

			// 创建进度指示器
			ProgressBar progressBar = new ProgressBar(context);
			progressBar.setIndeterminate(true);

			// 设置黄色进度条
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				progressBar.setIndeterminateTintList(ColorStateList.valueOf(Color.parseColor("#FFD54F")));
			}

			LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dp(context, 48), dp(context, 48));
			progressParams.gravity = Gravity.CENTER;
			progressParams.bottomMargin = dp(context, 16);
			mainLayout.addView(progressBar, progressParams);

			// 创建文本视图
			TextView messageText = new TextView(context);
			messageText.setText("正在解密图片...");
			messageText.setTextColor(Color.BLACK);
			messageText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
			messageText.setTypeface(null, Typeface.NORMAL);
			messageText.setGravity(Gravity.CENTER);

			LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			textParams.gravity = Gravity.CENTER;
			mainLayout.addView(messageText, textParams);

			// 设置对话框内容视图
			decryptProgressDialog.setContentView(mainLayout);

			// 显示对话框
			decryptProgressDialog.show();

			XposedBridge.log("解密进度对话框已显示（白底黑字样式）");
		} catch (Exception e) {
			XposedBridge.log("显示解密进度对话框失败: " + e.getMessage());
		}
	}

	// 创建黄色进度条Drawable
	private Drawable createYellowProgressDrawable(Context context) {
		try {
			// 使用旋转动画Drawable
			AnimationDrawable animationDrawable = new AnimationDrawable();

			// 创建黄色圆角矩形帧
			for (int i = 0; i < 12; i++) {
				GradientDrawable frame = new GradientDrawable();
				frame.setColor(Color.parseColor("#FFD54F")); // 黄色
				frame.setCornerRadius(dp(context, 16));
				frame.setSize(dp(context, 8), dp(context, 8));
				animationDrawable.addFrame(frame, 100);
			}

			animationDrawable.setOneShot(false);
			return animationDrawable;
		} catch (Exception e) {
			XposedBridge.log("创建进度条Drawable失败: " + e.getMessage());
			return new ColorDrawable(Color.parseColor("#FFD54F"));
		}
	}

	private void hideDecryptProgressDialog() {
		try {
			if (decryptProgressDialog != null && decryptProgressDialog.isShowing()) {
				decryptProgressDialog.dismiss();
				XposedBridge.log("解密进度对话框已隐藏");
			}
			decryptProgressDialog = null;
		} catch (Exception e) {
			XposedBridge.log("隐藏解密进度对话框失败: " + e.getMessage());
		}
	}

	// 收集解密后的文件信息
	private List<FileInfo> decryptTransitionImages() {
		List<FileInfo> decryptedFiles = new ArrayList<>();
		try {
			File imageDir = new File(IMAGE_DIR);
			if (!imageDir.exists() || !imageDir.isDirectory()) {
				XposedBridge.log("图片目录不存在");
				return decryptedFiles;
			}

			// 查找所有目标文件
			File[] targetFiles = imageDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(TARGET_SUFFIX);
				}
			});

			if (targetFiles == null || targetFiles.length == 0) {
				XposedBridge.log("未找到" + TARGET_SUFFIX + "文件");
				return decryptedFiles;
			}

			XposedBridge.log("找到" + targetFiles.length + "个待解密文件");

			// 解密每个文件并收集信息
			for (File encryptedFile : targetFiles) {
				try {
					File decryptedFile = decryptImage(encryptedFile);
					FileInfo fileInfo = new FileInfo();
					fileInfo.file = decryptedFile;
					fileInfo.originalFile = encryptedFile;
					fileInfo.createTime = encryptedFile.lastModified(); // 使用原文件的修改时间作为创建时间
					fileInfo.selected = false; // 默认未选中

					decryptedFiles.add(fileInfo);
				} catch (Exception e) {
					XposedBridge.log("解密文件失败: " + encryptedFile.getName() + " - " + e.getMessage());
				}
			}

			// 按创建时间降序排序
			Collections.sort(decryptedFiles, new Comparator<FileInfo>() {
				@Override
				public int compare(FileInfo f1, FileInfo f2) {
					return Long.compare(f2.createTime, f1.createTime);
				}
			});
		} catch (Exception e) {
			XposedBridge.log("解密图片过程出错: " + e.getMessage());
		}
		return decryptedFiles;
	}

	// 文件信息类
	class FileInfo {
		File file;
		File originalFile;
		long createTime;
		boolean selected;
	}

	// 可缩放图片视图类
	// 支持双指缩放
	// 始终不成功，还是放弃了
	public class TouchImageView extends ImageView {
		private Matrix matrix = new Matrix();
		private Matrix savedMatrix = new Matrix();

		private static final int NONE = 0;
		private static final int DRAG = 1;
		private static final int ZOOM = 2;
		private int mode = NONE;

		private PointF start = new PointF();
		private PointF mid = new PointF();
		private float oldDist = 1f;

		private float minScale = 1f;
		private float maxScale = 4f;

		private ScaleGestureDetector mScaleDetector;
		private GestureDetector mGestureDetector;

		public TouchImageView(Context context) {
			super(context);
			init(context);
		}

		public TouchImageView(Context context, AttributeSet attrs) {
			super(context, attrs);
			init(context);
		}

		private void init(Context context) {
			super.setClickable(true);

			// 设置缩放检测器
			mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());

			// 设置手势检测器（处理双击和滑动）
			mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
				@Override
				public boolean onDoubleTap(MotionEvent e) {
					// 双击切换缩放级别
					if (getCurrentZoom() == minScale) {
						setZoom(maxScale / 2, e.getX(), e.getY());
					} else {
						setZoom(minScale, e.getX(), e.getY());
					}
					return true;
				}

				@Override
				public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
					// 处理快速滑动
					return super.onFling(e1, e2, velocityX, velocityY);
				}
			});

			matrix.setTranslate(1f, 1f);
			setImageMatrix(matrix);
			setScaleType(ScaleType.MATRIX);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			// 处理手势事件
			mGestureDetector.onTouchEvent(event);

			// 处理缩放事件
			mScaleDetector.onTouchEvent(event);

			// 获取矩阵值
			float[] m = new float[9];
			matrix.getValues(m);
			PointF curr = new PointF(event.getX(), event.getY());

			switch (event.getAction() & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_DOWN :
					savedMatrix.set(matrix);
					start.set(curr);
					mode = DRAG;
					break;

				case MotionEvent.ACTION_POINTER_DOWN :
					oldDist = spacing(event);
					if (oldDist > 10f) {
						savedMatrix.set(matrix);
						midPoint(mid, event);
						mode = ZOOM;
					}
					break;

				case MotionEvent.ACTION_UP :
				case MotionEvent.ACTION_POINTER_UP :
					mode = NONE;
					break;

				case MotionEvent.ACTION_MOVE :
					if (mode == DRAG) {
						matrix.set(savedMatrix);
						matrix.postTranslate(curr.x - start.x, curr.y - start.y);
					} else if (mode == ZOOM) {
						float newDist = spacing(event);
						if (newDist > 10f) {
							matrix.set(savedMatrix);
							float scale = newDist / oldDist;
							matrix.postScale(scale, scale, mid.x, mid.y);
						}
					}
					break;
			}

			// 限制缩放和拖动范围
			checkAndFixScale();
			setImageMatrix(matrix);
			return true;
		}

		private void checkAndFixScale() {
			float[] m = new float[9];
			matrix.getValues(m);
			float scaleX = m[Matrix.MSCALE_X];
			float scaleY = m[Matrix.MSCALE_Y];
			float transX = m[Matrix.MTRANS_X];
			float transY = m[Matrix.MTRANS_Y];

			// 限制缩放范围
			if (scaleX < minScale) {
				matrix.postScale(minScale / scaleX, minScale / scaleY, getWidth() / 2, getHeight() / 2);
			} else if (scaleX > maxScale) {
				matrix.postScale(maxScale / scaleX, maxScale / scaleY, getWidth() / 2, getHeight() / 2);
			}

			// 限制拖动范围
			matrix.getValues(m);
			transX = m[Matrix.MTRANS_X];
			transY = m[Matrix.MTRANS_Y];

			if (getDrawable() != null) {
				float scaledWidth = getDrawable().getIntrinsicWidth() * m[Matrix.MSCALE_X];
				float scaledHeight = getDrawable().getIntrinsicHeight() * m[Matrix.MSCALE_Y];

				if (transX > 0)
					transX = 0;
				else if (transX + scaledWidth < getWidth())
					transX = getWidth() - scaledWidth;

				if (transY > 0)
					transY = 0;
				else if (transY + scaledHeight < getHeight())
					transY = getHeight() - scaledHeight;

				matrix.postTranslate(transX - m[Matrix.MTRANS_X], transY - m[Matrix.MTRANS_Y]);
			}
		}

		private float spacing(MotionEvent event) {
			float x = event.getX(0) - event.getX(1);
			float y = event.getY(0) - event.getY(1);
			return (float) Math.sqrt(x * x + y * y);
		}

		private void midPoint(PointF point, MotionEvent event) {
			float x = event.getX(0) + event.getX(1);
			float y = event.getY(0) + event.getY(1);
			point.set(x / 2, y / 2);
		}

		private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
			@Override
			public boolean onScale(ScaleGestureDetector detector) {
				float scaleFactor = detector.getScaleFactor();
				float[] m = new float[9];
				matrix.getValues(m);
				float scaleX = m[Matrix.MSCALE_X];
				float scaleY = m[Matrix.MSCALE_Y];

				if ((scaleX < maxScale || scaleFactor < 1f) && (scaleX > minScale || scaleFactor > 1f)) {
					matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
				}
				return true;
			}
		}

		public float getCurrentZoom() {
			float[] m = new float[9];
			matrix.getValues(m);
			return m[Matrix.MSCALE_X];
		}

		public float getMinZoom() {
			return minScale;
		}

		public float getMaxZoom() {
			return maxScale;
		}

		public void setZoom(float scale, float focusX, float focusY) {
			float[] m = new float[9];
			matrix.getValues(m);
			float currentScale = m[Matrix.MSCALE_X];
			matrix.postScale(scale / currentScale, scale / currentScale, focusX, focusY);
			checkAndFixScale();
			setImageMatrix(matrix);
		}
	}

	// 在图片解密完成后显示选择对话框
	private void showImageSelectionDialog(final Context context, final List<FileInfo> decryptedFiles) {
		try {
			final Dialog selectionDialog = new Dialog(context);
			selectionDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			selectionDialog.setCancelable(false);

			// 设置窗口属性
			Window window = selectionDialog.getWindow();
			if (window != null) {
				window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
				DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
				int dialogWidth = (int) (displayMetrics.widthPixels * 0.85);
				int dialogHeight = (int) (displayMetrics.heightPixels * 0.75);
				window.setLayout(dialogWidth, dialogHeight);
				window.setGravity(Gravity.CENTER);
			}

			// 创建主容器
			LinearLayout mainLayout = new LinearLayout(context);
			mainLayout.setOrientation(LinearLayout.VERTICAL);
			mainLayout.setPadding(dp(context, 20), dp(context, 20), dp(context, 20), dp(context, 20));

			// 设置白色背景和圆角
			GradientDrawable bg = new GradientDrawable();
			bg.setColor(Color.WHITE);
			bg.setCornerRadius(dp(context, 16));
			mainLayout.setBackground(bg);

			// 标题
			TextView title = new TextView(context);
			title.setText("选择要导出到相册的图片");
			title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
			title.setTextColor(Color.BLACK);
			title.setTypeface(null, Typeface.BOLD);
			title.setGravity(Gravity.CENTER);
			title.setPadding(0, 0, 0, dp(context, 16));
			mainLayout.addView(title);

			// 全选选项
			LinearLayout selectAllLayout = new LinearLayout(context);
			selectAllLayout.setOrientation(LinearLayout.HORIZONTAL);
			selectAllLayout.setGravity(Gravity.CENTER_VERTICAL);
			selectAllLayout.setPadding(0, 0, 0, dp(context, 16));

			final CheckBox selectAll = new CheckBox(context);
			selectAll.setText("全选");
			selectAll.setTextColor(Color.BLACK);
			selectAll.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

			// 设置复选框背景和颜色以提高可见性
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				selectAll.setButtonTintList(ColorStateList.valueOf(Color.parseColor("#FFC107")));
			}

			// 为复选框添加背景
			GradientDrawable checkBoxBg = new GradientDrawable();
			checkBoxBg.setColor(Color.parseColor("#F5F5F5"));
			checkBoxBg.setCornerRadius(dp(context, 4));
			selectAll.setBackground(checkBoxBg);
			selectAll.setPadding(dp(context, 8), dp(context, 4), dp(context, 8), dp(context, 4));

			selectAllLayout.addView(selectAll);
			mainLayout.addView(selectAllLayout);

			// 选择数量显示
			final TextView selectionCountText = new TextView(context);
			selectionCountText.setId(View.generateViewId());
			selectionCountText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
			selectionCountText.setTextColor(Color.GRAY);
			selectionCountText.setGravity(Gravity.CENTER);
			selectionCountText.setPadding(0, 0, 0, dp(context, 16));
			mainLayout.addView(selectionCountText);

			// 创建滚动视图
			ScrollView scrollView = new ScrollView(context);
			LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
			scrollView.setLayoutParams(scrollParams);

			// 图片网格容器
			LinearLayout gridLayout = new LinearLayout(context);
			gridLayout.setOrientation(LinearLayout.VERTICAL);
			scrollView.addView(gridLayout);

			// 添加图片项
			final List<CheckBox> checkBoxes = new ArrayList<>();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

			// 创建行容器
			LinearLayout currentRow = null;

			// 创建更新选择数量的方法
			final Runnable updateSelectionCount = new Runnable() {
				@Override
				public void run() {
					int selectedCount = 0;
					for (CheckBox cb : checkBoxes) {
						if (cb.isChecked()) {
							selectedCount++;
						}
					}
					selectionCountText.setText("已选择: " + selectedCount + " / " + decryptedFiles.size());

					// 更新全选状态
					boolean allChecked = (selectedCount == decryptedFiles.size());
					selectAll.setChecked(allChecked);
				}
			};

			// 初始更新选择数量
			updateSelectionCount.run();

			for (int i = 0; i < decryptedFiles.size(); i++) {
				final FileInfo fileInfo = decryptedFiles.get(i);
				final int index = i;

				// 每两个图片创建一个新行
				if (i % 2 == 0) {
					currentRow = new LinearLayout(context);
					currentRow.setOrientation(LinearLayout.HORIZONTAL);
					currentRow.setWeightSum(2);
					gridLayout.addView(currentRow);
				}

				// 创建单个图片项容器
				LinearLayout itemLayout = new LinearLayout(context);
				itemLayout.setOrientation(LinearLayout.VERTICAL);
				itemLayout.setGravity(Gravity.CENTER_HORIZONTAL);
				LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(0,
						LinearLayout.LayoutParams.WRAP_CONTENT, 1);
				itemParams.setMargins(i % 2 == 0 ? 0 : dp(context, 8), 0, 0, dp(context, 16));
				itemLayout.setLayoutParams(itemParams);

				// 创建图片容器，用于实现圆角效果
				FrameLayout imageContainer = new FrameLayout(context);
				imageContainer.setLayoutParams(
						new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 120)));

				// 创建圆角背景
				GradientDrawable imageBg = new GradientDrawable();
				imageBg.setColor(Color.LTGRAY);
				imageBg.setCornerRadius(dp(context, 8));
				imageContainer.setBackground(imageBg);

				// 创建图片视图
				final ImageView imageView = new ImageView(context);
				FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
						FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
				imageParams.setMargins(dp(context, 2), dp(context, 2), dp(context, 2), dp(context, 2));
				imageView.setLayoutParams(imageParams);
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

				// 创建圆角遮罩
				GradientDrawable roundCorners = new GradientDrawable();
				roundCorners.setCornerRadius(dp(context, 6));
				imageView.setBackground(roundCorners);
				imageView.setClipToOutline(true);

				// 异步加载图片
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							final Bitmap thumbnail = createThumbnail(fileInfo.file.getAbsolutePath(), dp(context, 200),
									dp(context, 200));

							new Handler(Looper.getMainLooper()).post(new Runnable() {
								@Override
								public void run() {
									if (thumbnail != null) {
										imageView.setImageBitmap(thumbnail);
									} else {
										// 如果加载失败，显示默认图标
										imageView.setBackgroundColor(Color.DKGRAY);
										imageView.setScaleType(ImageView.ScaleType.CENTER);
									}
								}
							});
						} catch (Exception e) {
							XposedBridge.log("加载缩略图失败: " + e.getMessage());
						}
					}
				}).start();

				imageContainer.addView(imageView);

				// 创建复选框
				final CheckBox checkBox = new CheckBox(context);
				checkBox.setChecked(fileInfo.selected);
				FrameLayout.LayoutParams checkBoxParams = new FrameLayout.LayoutParams(
						FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
				checkBoxParams.gravity = Gravity.TOP | Gravity.START;
				checkBoxParams.setMargins(dp(context, 8), dp(context, 8), 0, 0);
				checkBox.setLayoutParams(checkBoxParams);
				checkBox.setButtonTintList(ColorStateList.valueOf(Color.parseColor("#FFC107")));

				// 添加复选框状态变化监听
				checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						fileInfo.selected = isChecked;
						updateSelectionCount.run();
					}
				});

				imageContainer.addView(checkBox);
				checkBoxes.add(checkBox);

				// 创建文件名文本
				TextView fileName = new TextView(context);
				fileName.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT));
				fileName.setPadding(dp(context, 4), dp(context, 8), dp(context, 4), 0);
				fileName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
				fileName.setTextColor(Color.DKGRAY);
				fileName.setSingleLine(true);
				fileName.setEllipsize(TextUtils.TruncateAt.END);

				// 格式化文件名
				String originalName = fileInfo.file.getName();
				String displayName = originalName;
				if (originalName.length() > 20) {
					displayName = originalName.substring(0, 17) + "...";
				}
				fileName.setText(displayName + "\n" + sdf.format(new Date(fileInfo.createTime)));

				// 添加视图到项容器
				itemLayout.addView(imageContainer);
				itemLayout.addView(fileName);

				// 添加点击事件
				itemLayout.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						boolean newState = !checkBox.isChecked();
						checkBox.setChecked(newState);
					}
				});

				// 添加长按事件，放大图片
				itemLayout.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						showEnlargedImageDialog(context, fileInfo.file, index, decryptedFiles, checkBoxes);
						return true;
					}
				});

				// 将项添加到当前行
				if (currentRow != null) {
					currentRow.addView(itemLayout);
				}
			}

			mainLayout.addView(scrollView);

			// 按钮容器
			LinearLayout buttonLayout = new LinearLayout(context);
			buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
			buttonLayout.setGravity(Gravity.CENTER);
			buttonLayout.setPadding(0, dp(context, 16), 0, 0);

			Button cancelBtn = new Button(context);
			cancelBtn.setText("取消");
			cancelBtn.setTextColor(Color.WHITE);
			cancelBtn.setBackground(createButtonBackground(context, Color.parseColor("#9E9E9E")));
			cancelBtn.setPadding(dp(context, 24), dp(context, 8), dp(context, 24), dp(context, 8));

			Button confirmBtn = new Button(context);
			confirmBtn.setText("导出选中");
			confirmBtn.setTextColor(Color.WHITE);
			confirmBtn.setBackground(createButtonBackground(context, Color.parseColor("#FFC107")));
			confirmBtn.setPadding(dp(context, 24), dp(context, 8), dp(context, 24), dp(context, 8));

			// 设置按钮布局参数
			LinearLayout.LayoutParams cancelBtnParams = new LinearLayout.LayoutParams(0,
					LinearLayout.LayoutParams.WRAP_CONTENT, 1);
			cancelBtn.setLayoutParams(cancelBtnParams);

			LinearLayout.LayoutParams confirmBtnParams = new LinearLayout.LayoutParams(0,
					LinearLayout.LayoutParams.WRAP_CONTENT, 1);
			confirmBtnParams.setMargins(dp(context, 16), 0, 0, 0);
			confirmBtn.setLayoutParams(confirmBtnParams);

			buttonLayout.addView(cancelBtn);
			buttonLayout.addView(confirmBtn);
			mainLayout.addView(buttonLayout);

			selectionDialog.setContentView(mainLayout);

			// 全选/取消全选逻辑
			selectAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					for (CheckBox cb : checkBoxes) {
						cb.setChecked(isChecked);
					}
					updateSelectionCount.run();
				}
			});

			// 取消按钮逻辑
			cancelBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// 删除所有解密文件
					for (FileInfo fileInfo : decryptedFiles) {
						if (fileInfo.file.exists()) {
							fileInfo.file.delete();
						}
					}
					selectionDialog.dismiss();
					Toast.makeText(context, "已取消导出，解密文件已删除", Toast.LENGTH_SHORT).show();
				}
			});

			// 确认按钮逻辑
			confirmBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							int exportedCount = 0;
							int deletedCount = 0;

							// 创建相册目录
							File picturesDir = Environment
									.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
							File kuaiduizuoyeDir = new File(picturesDir, "KuaiDuizuoye");
							if (!kuaiduizuoyeDir.exists()) {
								kuaiduizuoyeDir.mkdirs();
							}

							for (int i = 0; i < decryptedFiles.size(); i++) {
								FileInfo fileInfo = decryptedFiles.get(i);
								if (checkBoxes.get(i).isChecked()) {
									// 移动到相册
									File destFile = new File(kuaiduizuoyeDir, fileInfo.file.getName());
									if (fileInfo.file.renameTo(destFile)) {
										// 通知媒体扫描器有新文件
										MediaScannerConnection.scanFile(context,
												new String[]{destFile.getAbsolutePath()}, new String[]{"image/jpeg"},
												null);
										exportedCount++;
									}
								} else {
									// 删除未选中的文件
									if (fileInfo.file.exists() && fileInfo.file.delete()) {
										deletedCount++;
									}
								}
							}

							final int finalExported = exportedCount;
							final int finalDeleted = deletedCount;

							// 在主线程显示结果
							new Handler(Looper.getMainLooper()).post(new Runnable() {
								@Override
								public void run() {
									selectionDialog.dismiss();
									Toast.makeText(context, "导出成功: " + finalExported + "张, 删除: " + finalDeleted + "张",
											Toast.LENGTH_LONG).show();
								}
							});
						}
					}).start();
				}
			});

			selectionDialog.show();
		} catch (Exception e) {
			XposedBridge.log("显示图片选择对话框失败: " + e.getMessage());
		}
	}

	// 修改后的 showEnlargedImageDialog 方法
	private void showEnlargedImageDialog(final Context context, final File imageFile, final int currentIndex,
			final List<FileInfo> decryptedFiles, final List<CheckBox> checkBoxes) {
		try {
			final Dialog enlargeDialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
			enlargeDialog.setCancelable(true);

			// 创建根布局
			final FrameLayout rootLayout = new FrameLayout(context);
			rootLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT));
			rootLayout.setBackgroundColor(Color.BLACK);

			// 创建可缩放的图片视图
			final TouchImageView imageView = new TouchImageView(context);
			imageView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
					FrameLayout.LayoutParams.MATCH_PARENT));
			imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

			// 创建顶部工具栏
			LinearLayout toolbar = new LinearLayout(context);
			toolbar.setOrientation(LinearLayout.HORIZONTAL);
			toolbar.setGravity(Gravity.CENTER_VERTICAL);
			toolbar.setBackgroundColor(Color.parseColor("#80000000"));
			FrameLayout.LayoutParams toolbarParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
					dp(context, 56));
			toolbarParams.gravity = Gravity.TOP;
			toolbar.setLayoutParams(toolbarParams);
			toolbar.setPadding(dp(context, 16), 0, dp(context, 16), 0);

			// 返回按钮
			ImageButton backButton = new ImageButton(context);
			backButton.setImageResource(android.R.drawable.ic_menu_revert);
			backButton.setBackground(null);
			backButton.setColorFilter(Color.WHITE);
			backButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					enlargeDialog.dismiss();
				}
			});
			toolbar.addView(backButton);

			// 文件名显示
			final TextView fileNameText = new TextView(context);
			fileNameText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
			fileNameText.setTextColor(Color.WHITE);
			fileNameText.setPadding(dp(context, 16), 0, 0, 0);
			fileNameText.setSingleLine(true);
			fileNameText.setEllipsize(TextUtils.TruncateAt.MIDDLE);
			LinearLayout.LayoutParams fileNameParams = new LinearLayout.LayoutParams(0,
					LinearLayout.LayoutParams.WRAP_CONTENT, 1);
			toolbar.addView(fileNameText, fileNameParams);

			// 选择状态文本
			final TextView selectionText = new TextView(context);
			selectionText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
			selectionText.setTextColor(Color.WHITE);
			selectionText.setPadding(dp(context, 16), 0, 0, 0);
			toolbar.addView(selectionText);

			// 选择复选框
			final CheckBox selectionCheckBox = new CheckBox(context);
			selectionCheckBox.setButtonTintList(ColorStateList.valueOf(Color.WHITE));
			toolbar.addView(selectionCheckBox);

			rootLayout.addView(imageView);
			rootLayout.addView(toolbar);

			// 添加进度指示器
			final ProgressBar progressBar = new ProgressBar(context);
			progressBar.setIndeterminate(true);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				progressBar.setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));
			}
			FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			progressParams.gravity = Gravity.CENTER;
			rootLayout.addView(progressBar, progressParams);

			// 当前显示的图片索引
			final int[] currentImageIndex = {currentIndex};

			// 更新UI方法
			final Runnable updateUI = new Runnable() {
				@Override
				public void run() {
					try {
						// 移除复选框监听器避免循环调用
						selectionCheckBox.setOnCheckedChangeListener(null);

						// 更新文件名
						updateFileName(fileNameText, decryptedFiles.get(currentImageIndex[0]).file.getName());

						// 更新复选框状态
						selectionCheckBox.setChecked(checkBoxes.get(currentImageIndex[0]).isChecked());

						// 更新选择文本
						updateSelectionText(selectionText, checkBoxes, currentImageIndex[0], decryptedFiles.size());

						// 重新设置复选框监听器
						selectionCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
							@Override
							public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
								checkBoxes.get(currentImageIndex[0]).setChecked(isChecked);
								updateSelectionText(selectionText, checkBoxes, currentImageIndex[0],
										decryptedFiles.size());
							}
						});
					} catch (Exception e) {
						XposedBridge.log("更新UI失败: " + e.getMessage());
					}
				}
			};

			// 初始化UI
			updateUI.run();

			// 手势检测器，用于左右滑动切换图片
			final GestureDetector gestureDetector = new GestureDetector(context,
					new GestureDetector.SimpleOnGestureListener() {
						private static final int SWIPE_THRESHOLD = 100;
						private static final int SWIPE_VELOCITY_THRESHOLD = 100;

						@Override
						public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
							float diffX = e2.getX() - e1.getX();
							float diffY = e2.getY() - e1.getY();
							if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > SWIPE_THRESHOLD
									&& Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
								if (diffX > 0) {
									// 向右滑，显示上一张
									if (currentImageIndex[0] > 0) {
										currentImageIndex[0]--;
										loadImageToTouchView(enlargeDialog, rootLayout, imageView, progressBar,
												decryptedFiles.get(currentImageIndex[0]).file);
										updateUI.run();
									}
								} else {
									// 向左滑，显示下一张
									if (currentImageIndex[0] < decryptedFiles.size() - 1) {
										currentImageIndex[0]++;
										loadImageToTouchView(enlargeDialog, rootLayout, imageView, progressBar,
												decryptedFiles.get(currentImageIndex[0]).file);
										updateUI.run();
									}
								}
								return true;
							}
							return false;
						}

						@Override
						public boolean onDoubleTap(MotionEvent e) {
							if (imageView.getCurrentZoom() == imageView.getMinZoom()) {
								imageView.setZoom(imageView.getMaxZoom() / 2, e.getX(), e.getY());
							} else {
								imageView.setZoom(imageView.getMinZoom(), e.getX(), e.getY());
							}
							return true;
						}

						@Override
						public boolean onSingleTapConfirmed(MotionEvent e) {
							enlargeDialog.dismiss();
							return true;
						}
					});

			imageView.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					gestureDetector.onTouchEvent(event);
					return true;
				}
			});

			// 异步加载大图
			loadImageToTouchView(enlargeDialog, rootLayout, imageView, progressBar, imageFile);

			enlargeDialog.setContentView(rootLayout);
			enlargeDialog.show();

		} catch (Exception e) {
			XposedBridge.log("显示放大图片对话框失败: " + e.getMessage());
		}
	}

	// updateSelectionText 方法
	private void updateSelectionText(TextView textView, List<CheckBox> checkBoxes, int currentIndex, int totalCount) {
		if (textView != null) {
			int selectedCount = 0;
			for (CheckBox cb : checkBoxes) {
				if (cb.isChecked()) {
					selectedCount++;
				}
			}
			textView.setText((currentIndex + 1) + "/" + totalCount + " (已选: " + selectedCount + ")");
		}
	}

	// 加载图片到TouchImageView
	private void loadImageToTouchView(final Dialog dialog, final FrameLayout rootLayout, final TouchImageView imageView,
			final ProgressBar progressBar, final File imageFile) {
		try {
			// 显示进度条
			progressBar.setVisibility(View.VISIBLE);

			// 异步加载图片
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						// 加载完整图片
						BitmapFactory.Options options = new BitmapFactory.Options();
						options.inSampleSize = 1;
						final Bitmap fullImage = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								// 隐藏进度条
								progressBar.setVisibility(View.GONE);

								if (fullImage != null) {
									imageView.setImageBitmap(fullImage);
									// 重置缩放状态
									imageView.setZoom(imageView.getMinZoom(), imageView.getWidth() / 2,
											imageView.getHeight() / 2);
								} else {
									// 加载失败显示错误信息
									TextView errorText = new TextView(dialog.getContext());
									errorText.setText("无法加载图片");
									errorText.setTextColor(Color.WHITE);
									errorText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
									errorText.setGravity(Gravity.CENTER);
									rootLayout.removeAllViews();
									rootLayout.addView(errorText);
								}
							}
						});
					} catch (OutOfMemoryError e) {
						XposedBridge.log("加载大图时内存不足: " + e.getMessage());
						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								progressBar.setVisibility(View.GONE);

								// 尝试使用缩略图
								Bitmap thumbnail = createThumbnail(imageFile.getAbsolutePath(), rootLayout.getWidth(),
										rootLayout.getHeight());
								if (thumbnail != null) {
									imageView.setImageBitmap(thumbnail);
								} else {
									TextView errorText = new TextView(dialog.getContext());
									errorText.setText("图片太大，无法加载");
									errorText.setTextColor(Color.WHITE);
									errorText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
									errorText.setGravity(Gravity.CENTER);
									rootLayout.removeAllViews();
									rootLayout.addView(errorText);
								}
							}
						});
					} catch (Exception e) {
						XposedBridge.log("加载大图失败: " + e.getMessage());
						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								progressBar.setVisibility(View.GONE);
							}
						});
					}
				}
			}).start();
		} catch (Exception e) {
			XposedBridge.log("加载图片失败: " + e.getMessage());
		}
	}

	// 加载图片
	private void loadImage(final Dialog dialog, final FrameLayout rootLayout, final TouchImageView imageView,
			final TextView fileNameText, final TextView selectionText, final CheckBox selectionCheckBox,
			final ProgressBar progressBar, final File imageFile, final int currentIndex,
			final List<FileInfo> decryptedFiles, final List<CheckBox> checkBoxes) {
		try {
			// 显示进度条
			progressBar.setVisibility(View.VISIBLE);

			// 更新文件名
			updateFileName(fileNameText, imageFile.getName());

			// 更新选择状态
			selectionCheckBox.setChecked(checkBoxes.get(currentIndex).isChecked());
			updateSelectionText(selectionText, checkBoxes, currentIndex, decryptedFiles.size());

			// 异步加载图片
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						// 加载完整图片
						BitmapFactory.Options options = new BitmapFactory.Options();
						options.inSampleSize = 1;
						final Bitmap fullImage = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								// 隐藏进度条
								progressBar.setVisibility(View.GONE);

								if (fullImage != null) {
									imageView.setImageBitmap(fullImage);
									// 重置缩放状态
									imageView.setZoom(imageView.getMinZoom(), imageView.getWidth() / 2,
											imageView.getHeight() / 2);
								} else {
									// 加载失败显示错误信息
									TextView errorText = new TextView(dialog.getContext());
									errorText.setText("无法加载图片");
									errorText.setTextColor(Color.WHITE);
									errorText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
									errorText.setGravity(Gravity.CENTER);
									rootLayout.removeAllViews();
									rootLayout.addView(errorText);
								}
							}
						});
					} catch (OutOfMemoryError e) {
						XposedBridge.log("加载大图时内存不足: " + e.getMessage());
						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								progressBar.setVisibility(View.GONE);

								// 尝试使用缩略图
								Bitmap thumbnail = createThumbnail(imageFile.getAbsolutePath(), rootLayout.getWidth(),
										rootLayout.getHeight());
								if (thumbnail != null) {
									imageView.setImageBitmap(thumbnail);
								} else {
									TextView errorText = new TextView(dialog.getContext());
									errorText.setText("图片太大，无法加载");
									errorText.setTextColor(Color.WHITE);
									errorText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
									errorText.setGravity(Gravity.CENTER);
									rootLayout.removeAllViews();
									rootLayout.addView(errorText);
								}
							}
						});
					} catch (Exception e) {
						XposedBridge.log("加载大图失败: " + e.getMessage());
						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								progressBar.setVisibility(View.GONE);
							}
						});
					}
				}
			}).start();
		} catch (Exception e) {
			XposedBridge.log("加载图片失败: " + e.getMessage());
		}
	}

	// 更新文件名显示
	private void updateFileName(TextView textView, String fileName) {
		if (textView != null) {
			// 格式化文件名
			String displayName = fileName;
			if (fileName.length() > 25) {
				displayName = fileName.substring(0, 22) + "...";
			}
			textView.setText(displayName);
		}
	}

	// 处理大图
	private Bitmap createThumbnail(String filePath, int reqWidth, int reqHeight) {
		try {
			// 第一次解码，只获取图片尺寸
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(filePath, options);

			// 计算采样率
			options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

			// 第二次解码，获取缩略图
			options.inJustDecodeBounds = false;
			options.inPreferredConfig = Bitmap.Config.RGB_565; // 使用更少内存的配置
			return BitmapFactory.decodeFile(filePath, options);
		} catch (OutOfMemoryError e) {
			XposedBridge.log("创建缩略图时内存不足: " + e.getMessage());
			// 尝试使用更大的采样率
			try {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(filePath, options);

				// 使用更大的采样率
				options.inSampleSize = calculateInSampleSize(options, reqWidth / 2, reqHeight / 2);

				// 第二次解码
				options.inJustDecodeBounds = false;
				options.inPreferredConfig = Bitmap.Config.RGB_565;
				return BitmapFactory.decodeFile(filePath, options);
			} catch (Exception ex) {
				XposedBridge.log("再次尝试创建缩略图失败: " + ex.getMessage());
				return null;
			}
		} catch (Exception e) {
			XposedBridge.log("创建缩略图失败: " + e.getMessage());
			return null;
		}
	}

	// 计算采样率
	private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

	// 工具方法：dp转px
	private int dp(Context context, float dp) {
		float density = context.getResources().getDisplayMetrics().density;
		return (int) (dp * density + 0.5f);
	}

	// 创建按钮背景
	private Drawable createButtonBackground(Context context, int color) {
		GradientDrawable drawable = new GradientDrawable();
		drawable.setColor(color);
		drawable.setCornerRadius(dp(context, 8));
		return drawable;
	}

	// 解密工具

	// 解密单个图片文件
	// 将解密后的文件保存在原目录
	private File decryptImage(File encryptedFile) throws IOException {
		// 读取文件内容
		byte[] data = readFileToByteArray(encryptedFile);

		// 查找JPEG头
		byte[] head = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xDB};
		int first = indexOf(data, head, 0);
		if (first == -1) {
			throw new IOException("未找到JPEG头");
		}

		int second = indexOf(data, head, first + 1);
		if (second == -1) {
			throw new IOException("未找到第二个JPEG头");
		}

		// 计算要删除的部分
		int firstEnd = second;
		int secondEnd = second + (firstEnd - first);

		// 构建新数据
		byte[] cleaned = new byte[data.length - (secondEnd - second)];
		System.arraycopy(data, 0, cleaned, 0, second);
		System.arraycopy(data, secondEnd, cleaned, second, data.length - secondEnd);

		// 生成输出文件名（添加_DECRYPTED后缀）
		String originalName = encryptedFile.getName();
		String baseName = originalName.substring(0, originalName.indexOf(TARGET_SUFFIX));
		String decryptedName = baseName + "_DECRYPTED.jpg";

		// 创建输出文件（保存在原目录）
		File outputFile = new File(encryptedFile.getParentFile(), decryptedName);

		// 写入解密后的文件
		writeByteArrayToFile(outputFile, cleaned);

		XposedBridge.log("已解密并保存到原目录: " + outputFile.getName());
		return outputFile;
	}

	// 在字节数组中查找模式
	private int indexOf(byte[] array, byte[] pattern, int fromIndex) {
		outer : for (int i = fromIndex; i <= array.length - pattern.length; i++) {
			for (int j = 0; j < pattern.length; j++) {
				if (array[i + j] != pattern[j]) {
					continue outer;
				}
			}
			return i;
		}
		return -1;
	}

	// 读取文件到字节数组
	private byte[] readFileToByteArray(File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len;
		while ((len = fis.read(buffer)) != -1) {
			bos.write(buffer, 0, len);
		}
		fis.close();
		return bos.toByteArray();
	}

	// 将字节数组写入文件
	private void writeByteArrayToFile(File file, byte[] data) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(data);
		fos.close();
	}

	// ==================== 设置读取（模块 App 写入，Hook 侧读取）====================

	/** 模块包名：设置由模块 App 的 SharedPreferences 提供 */
	private static final String MODULE_PACKAGE = "com.kite.kuaisnapplus";

	private static XSharedPreferences sSharedPrefs;
	private static boolean sPrefsInited = false;
	private static final Map<String, Boolean> sSettingCache = new HashMap<String, Boolean>();

	/**
	 * 初始化跨进程设置（XSharedPreferences）。
	 * 依赖 Manifest 中的 xposedsharedprefs 元数据；模块未激活或不受支持时保持为 null，
	 * 由 getBooleanSetting 回退到默认值。
	 */
	private static void initSharedPrefs() {
		if (sPrefsInited) {
			return;
		}
		sPrefsInited = true;
		try {
			XSharedPreferences prefs = new XSharedPreferences(MODULE_PACKAGE, PREFS_NAME);
			prefs.reload();
			// API 82 没有 isReadable()，用文件可读性判断（读不到时 getBoolean 会返回默认值）
			File prefsFile = prefs.getFile();
			if (prefsFile != null && prefsFile.canRead()) {
				sSharedPrefs = prefs;
				XposedBridge.log("快怼+: 已读取模块设置");
			} else {
				XposedBridge.log("快怼+: 模块设置不可读，将使用默认值");
			}
		} catch (Throwable t) {
			XposedBridge.log("快怼+: 初始化模块设置失败 " + t);
		}
	}

	/**
	 * 读取布尔设置。
	 *
	 * 值来自模块 App 的 SharedPreferences（经 XSharedPreferences 跨进程读取），
	 * 读不到时回退到目标应用内的旧设置（兼容从旧版本升级的用户），最后回退默认值。
	 * 结果做内存缓存：设置改动需重启作用域软件后生效。
	 */
	private boolean getBooleanSetting(Context context, String key, boolean defaultValue) {
		Boolean cached = sSettingCache.get(key);
		if (cached != null) {
			return cached.booleanValue();
		}

		boolean value = defaultValue;
		initSharedPrefs();
		if (sSharedPrefs != null) {
			try {
				value = sSharedPrefs.getBoolean(key, defaultValue);
			} catch (Throwable t) {
				XposedBridge.log("读取模块设置失败 " + key + ": " + t);
			}
		} else if (context != null) {
			try {
				value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
						.getBoolean(key, defaultValue);
			} catch (Throwable t) {
				XposedBridge.log("读取旧设置失败 " + key + ": " + t);
			}
		}

		sSettingCache.put(key, Boolean.valueOf(value));
		return value;
	}

	private void setupVipHooks(ClassLoader classLoader) {
		try {
			//hook① - 方法一
			Class<?> shareresourceCollectConfigClass = XposedHelpers
					.findClass("com.kuaiduizuoye.scan.common.net.model.v1.ShareresourceCollectConfig", classLoader);

			XposedHelpers.findAndHookMethod(spec.clsVipDialogShow, classLoader, spec.mtdVipDialogShow,
					shareresourceCollectConfigClass, new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam parameter) throws Throwable {
							parameter.setResult(false);
						}
					});

			//hook② - 方法二
			XposedHelpers.findAndHookMethod(spec.clsVipStateStr, classLoader, spec.mtdVipStateStr,
					new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam parameter) throws Throwable {
							parameter.setResult("1");
						}
					});

			//hook③ - 方法三
			XposedHelpers.findAndHookMethod(CLS_MINE_FRAGMENT, classLoader,
					spec.mtdMineFragmentVipStr, new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam parameter) throws Throwable {
							parameter.setResult("1");
						}
					});

			//hook④ - 方法四
			XposedHelpers.findAndHookMethod(CLS_MINE_UTIL, classLoader, spec.mtdMineUtilVipStr,
					new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam parameter) throws Throwable {
							parameter.setResult("1");
						}
					});

			//hook⑤ - 方法五
			XposedHelpers.findAndHookMethod(spec.clsFastAdConfig, classLoader, spec.mtdFastAdVip,
					new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam parameter) throws Throwable {
							parameter.setResult(true);
						}
					});

			XposedBridge.log("VIP相关Hook设置成功");
		} catch (Exception e) {
			XposedBridge.log("VIP Hook设置失败: " + e.getMessage());
		}
	}

	private void setupRotateHooks(ClassLoader classLoader) {
		try {
			//hook⑥ - 方法六：vip.status 原始值，强制为 1（会员有效）
			XposedHelpers.findAndHookMethod(spec.clsRawVipStatus, classLoader, spec.mtdRawVipStatus,
					new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam parameter) throws Throwable {
							parameter.setResult(1);
						}
					});

			//hook⑥b - 布尔型会员判断（“横屏浏览”等 VIP 功能的开关）
			XposedHelpers.findAndHookMethod(spec.clsRawVipStatus, classLoader, spec.mtdVipGate,
					new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam parameter) throws Throwable {
							parameter.setResult(true);
						}
					});
			XposedBridge.log("横屏旋转Hook设置成功");
		} catch (Exception e) {
			XposedBridge.log("横屏旋转Hook设置失败: " + e.getMessage());
		}
	}

	private void setupHdHooks(ClassLoader classLoader) {
		try {
			//hook⑦ - 方法七
			XposedHelpers.findAndHookMethod(spec.clsHdPrivilege, classLoader, spec.mtdHdPrivilege,
					new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam parameter) throws Throwable {
							parameter.setResult(true);
						}
					});
			XposedBridge.log("高清内容Hook设置成功");
		} catch (Exception e) {
			XposedBridge.log("高清内容Hook设置失败: " + e.getMessage());
		}
	}

	private void setupVipBadgeHooks(final ClassLoader classLoader) {
		try {
			//hook⑧ - MineAiUserLoginView setVipIcon方法
			XposedHelpers.findAndHookMethod(CLS_MINE_AI_LOGIN_VIEW,
					classLoader, "setVipIcon", new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam parameter) throws Throwable {
							forceVipStatus(parameter.thisObject);
						}
					});

			//hook⑨ - MineUserLoginView setVipIcon方法
			XposedHelpers.findAndHookMethod(CLS_MINE_LOGIN_VIEW, classLoader,
					"setVipIcon", new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam parameter) throws Throwable {
							forceVipStatus(parameter.thisObject);
						}
					});
			XposedBridge.log("会员金标Hook设置成功");
		} catch (Exception e) {
			XposedBridge.log("会员金标Hook设置失败: " + e.getMessage());
		}
	}

	/**
	 * 将视图持有的用户信息中的 VIP 状态强制为 1，使会员金标显示为已开通。
	 * 注意：不能自行构造 vip 对象，其类型由快对内部持有，凭空 new 一个无法被识别。
	 */
	private void forceVipStatus(Object view) {
		try {
			Object mUserInfo = XposedHelpers.getObjectField(view, "mUserInfo");
			if (mUserInfo == null) {
				return;
			}
			Object vip = XposedHelpers.getObjectField(mUserInfo, "vip");
			if (vip == null) {
				XposedBridge.log("会员金标: mUserInfo.vip 为空，跳过");
				return;
			}
			XposedHelpers.setIntField(vip, "status", 1);
		} catch (Throwable t) {
			XposedBridge.log("会员金标: 强制会员状态失败 " + t);
		}
	}

	/**
	 * 去除图片水印。
	 * 快对 7.7.0 起，网页版图片浏览页（CommonWebPictureBrowseActivity）在
	 * needWatermark 为真时，会把本地水印图（drawable）按 800dp×500dp 网格、
	 * 奇偶行错位、每个旋转 -30° 平铺合成到显示用的位图上。
	 * 这里直接返回原图，跳过整个平铺绘制过程。
	 */
	private void setupWatermarkHooks(ClassLoader classLoader) {
		try {
			if (spec.mtdWatermark == null) {
				XposedBridge.log("图片水印Hook: 快对 " + spec.label + " 无图片水印逻辑，跳过");
				return;
			}
			XposedHelpers.findAndHookMethod(CLS_WEB_PICTURE_BROWSE, classLoader, spec.mtdWatermark,
					Bitmap.class, new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
							// 直接把原图返回，不再合成水印
							param.setResult(param.args[0]);
						}
					});
			XposedBridge.log("图片水印Hook设置成功");
		} catch (Throwable t) {
			XposedBridge.log("图片水印Hook设置失败: " + t);
		}
	}

	private void setupScreenCaptureHooks(ClassLoader classLoader) {
		try {
			// hook⑩ - 解除截/录屏限制
			XposedHelpers.findAndHookMethod(
					CLS_BOOK_BROWSE,
					classLoader, spec.mtdScreenCapture, new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
							param.setResult(null);
							XposedBridge.log("已解除截屏/录屏限制");
						}
					});
			XposedBridge.log("截屏限制Hook设置成功");
		} catch (Exception e) {
			XposedBridge.log("截屏限制Hook设置失败: " + e.getMessage());
		}
	}

	// 判断是否为设置页面的方法
	
	// 查找Activity中的WebView

	// 递归查找WebView

	// 讲解视频Hook
	private void setupVideoExplanationHooks(ClassLoader classLoader) {
		try {
			XposedBridge.log("开始设置讲解视频解锁Hook");

			// Hook MultipleVideoBean的getHasBuy方法，使其返回1
			XposedHelpers.findAndHookMethod(CLS_MULTI_VIDEO_BEAN, classLoader,
					"getHasBuy", new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable {
							param.setResult(1);
							XposedBridge.log("成功，讲解时间延长");
						}
					});

			// 屏蔽视频播放器特定元素
			// Hook VideoPlayerActivity的onCreate方法
			XposedHelpers.findAndHookMethod(CLS_VIDEO_ACTIVITY, classLoader,
					"onCreate", Bundle.class, new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable {
							final Activity activity = (Activity) param.thisObject;

							// 延迟执行以确保视图已加载
							new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
								@Override
								public void run() {
									try {
										if (activity.isFinishing() || activity.isDestroyed()) {
											return;
										}

										// 查找并移除讲解视频的遮挡 WebView
										View targetElement = activity.findViewById(spec.idVideoMask);
										if (targetElement != null) {
											ViewGroup parent = (ViewGroup) targetElement.getParent();
											if (parent != null) {
												parent.removeView(targetElement);
												XposedBridge.log("成功屏蔽视频播放器元素 (ID: " + Integer.toHexString(spec.idVideoMask)
														+ ")");
											} else {
												XposedBridge.log("视频播放器元素的父视图为空");
											}
										} else {
											XposedBridge.log("未找到视频播放器元素 (ID: " + Integer.toHexString(spec.idVideoMask)
													+ ")");

										}
									} catch (Exception e) {
										XposedBridge.log("屏蔽视频播放器元素失败: " + e.getMessage());
									}
								}
							}, 1000); // 延迟1秒执行
						}
					});

			XposedBridge.log("讲解视频解锁Hook设置成功");
		} catch (Exception e) {
			XposedBridge.log("讲解视频解锁Hook设置失败: " + e.getMessage());
		}
	}




	private int dp(Context ctx, int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics());
	}

	/**
	 * 显示启动提示。
	 *
	 * 是否显示由模块设置 block_startup_message 决定（经 XSharedPreferences 读取）；
	 * 旧版本基于 .Kuaisnap/toast_config 公共文件的开关已废弃并移除。
	 */
	private void jiguromessage(String message) {
		if (Context == null || Context.isFinishing()) {
			return;
		}
		if (getBooleanSetting(Context, "block_startup_message", false)) {
			XposedBridge.log("启动提示已被设置屏蔽: " + message);
			return;
		}
		try {
			Toast.makeText(Context, message, Toast.LENGTH_SHORT).show();
			XposedBridge.log("显示启动提示: " + message);
		} catch (Exception e) {
			XposedBridge.log("提示异常:" + e);
		}
	}

}

