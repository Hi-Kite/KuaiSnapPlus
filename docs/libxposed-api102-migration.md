# 迁移到 libxposed API 102 方案

> 本文档记录「快怼」从经典 XposedBridge API 迁移到 LSPosed 现代 API 的完整方案。
> 当前状态：**未迁移**，模块仍基于经典 API 82（详见 README「框架与兼容性」）。
> 触发条件：LSPosed 移除对经典 API 的兼容层，或需要使用现代 API 的能力时。

## 1. 背景：两套 API 的区别

| 项目 | 经典 API（现状） | libxposed API 102 |
|---|---|---|
| 包名 | `de.robv.android.xposed` | `io.github.libxposed.api` |
| 版本 | 82（最终版） | 102.0.0（2026-06-14） |
| 引入 | `app/libs/compile_only/xposed-api-82_compileonly.jar` | `compileOnly("io.github.libxposed:api:102.0.0")` |
| Java 入口 | `assets/xposed_init` | `META-INF/xposed/java_init.list` |
| 入口基类 | `IXposedHookLoadPackage` | `io.github.libxposed.api.XposedModule` |
| 元数据 | Manifest 的 `xposedmodule` 等 meta-data | `META-INF/xposed/module.prop` + `scope.list`，名称/描述用 `android:label` / `android:description` |
| Hook 模型 | `XposedHelpers.findAndHookMethod(...)` | 拦截器链 `hook(m).intercept(chain -> ...)` |
| 反射工具 | `XposedHelpers` | **已移除**，需自行封装原生反射 |
| 资源 Hook | 支持 | **已移除**（本模块未使用，无影响） |
| 最低 SDK | 21 | **26** |
| 自身被 Hook | 支持 | **不支持**（模块 App 不再被 Hook） |

参考资料：

- API 仓库（Apache-2.0）：<https://github.com/libxposed/api>
- 官方迁移指南：<https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API>
- API 参考：<https://libxposed.github.io/api/>
- 官方示例模块：<https://github.com/libxposed/example>
- 辅助开发库：<https://github.com/libxposed/helper>

## 2. 影响评估

**改造范围仅限「Hook 接入层」**，业务逻辑可以原样保留：

| 内容 | 是否受影响 |
|---|---|
| `VerSpec` 版本配置表（类名/方法名/资源 ID） | ❌ 不受影响，原样保留 |
| 各 Hook 的业务逻辑（`setupVipHooks` 等回调体） | ❌ 逻辑不变，仅回调写法改变 |
| 图片解密、导出相册、设置面板 UI | ❌ 不受影响 |
| `XposedHelpers.*` 调用（26+2+3+1+1 处） | ✅ 需改写 |
| `XC_MethodHook` 回调（28 处） | ✅ 需改写为拦截器 |
| `XposedBridge.log`（130 处） | ✅ 需改为 `XposedModule.log(...)` |
| 入口类与元数据 | ✅ 需改写 |
| `ModuleStatus` 自身激活检测 | ✅ 需改用服务通信 |

## 3. 迁移步骤

### 3.1 依赖与 SDK

```gradle
android {
    defaultConfig {
        minSdkVersion 26          // 由 21 提升
    }
}

dependencies {
    compileOnly "io.github.libxposed:api:102.0.0"
}
```

删除 `app/libs/compile_only/xposed-api-82_compileonly.jar`。

### 3.2 元数据文件

新建 `app/src/main/resources/META-INF/xposed/`：

`java_init.list`（入口类全限定名，一行一个）：

```
xposed
```

`scope.list`（作用域，一行一个包名）：

```
com.kuaiduizuoye.scan
```

`module.prop`：

```properties
minApiVersion=101
targetApiVersion=102
staticScope=true
```

`AndroidManifest.xml` 中**删除** `xposedmodule` / `xposeddescription` / `xposedscope` / `xposedminversion` 四个 meta-data；模块名称改用 `android:label`、描述改用 `android:description`。同时删除 `app/src/main/assets/xposed_init`。

### 3.3 入口类

```java
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

public class xposed extends XposedModule {
    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        // 不要在此初始化，框架尚未就绪
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!"com.kuaiduizuoye.scan".equals(param.getPackageName())) return;
        Context context = getModuleContext();
        ClassLoader cl = param.getClassLoader();
        spec = selectSpec(context, cl);
        setupAllHooks(context, cl);
    }
}
```

### 3.4 Hook 写法对照

这是改造量最大的部分，但机械且可批量进行：

```java
// —— 经典 API：setResult 覆盖返回值 ——
XposedHelpers.findAndHookMethod(CLS_MINE_UTIL, classLoader, "i", new XC_MethodHook() {
    @Override protected void afterHookedMethod(MethodHookParam p) { p.setResult("1"); }
});

// —— 现代 API ——
Method m = ReflectUtil.findMethod(CLS_MINE_UTIL, cl, "i");   // 需自建反射工具
hook(m).intercept(chain -> {
    chain.proceed();     // 先执行原方法
    return "1";          // 覆盖返回值
});
```

```java
// —— 经典 API：beforeHookedMethod + setResult(null) 阻止原方法 ——
XposedHelpers.findAndHookMethod(CLS_BOOK_BROWSE, classLoader, "W3", new XC_MethodHook() {
    @Override protected void beforeHookedMethod(MethodHookParam p) { p.setResult(null); }
});

// —— 现代 API：不调用 proceed 即为阻止原方法 ——
hook(m).intercept(chain -> null);
```

对照规则：

| 经典 API | 现代 API |
|---|---|
| `beforeHookedMethod` + `setResult(v)` | 直接 `return v;`（不调用 `proceed()`） |
| `beforeHookedMethod` 仅观察/改参数 | 改 `chain.getArgs()` 后 `return chain.proceed();` |
| `afterHookedMethod` + `setResult(v)` | `chain.proceed(); return v;` |
| `afterHookedMethod` 仅观察 | `var r = chain.proceed(); ...; return r;` |
| `param.args[i] = x` | `chain.getArgs().set(i, x)` |

### 3.5 反射工具

现代 API 不再提供 `XposedHelpers`，需自建一个小工具类（约 50 行，覆盖当前用到的 5 个方法）：

```java
static Object getObjectField(Object obj, String name) { ... }
static void   setIntField(Object obj, String name, int v) { ... }
static Object callMethod(Object obj, String name) { ... }
static Class<?> findClass(String name, ClassLoader cl) { ... }
```

对应替换：`XposedHelpers.getObjectField` ×3、`setIntField` ×1、`callMethod` ×1、`findClass` ×2。

### 3.6 激活状态检测

现代 API 下**模块 App 不再被 Hook**，现有的做法（Hook 自己写 `ModuleStatus.activated`）会失效。改用 `libxposed/service`：

```gradle
implementation "io.github.libxposed:service:1.0.0"
```

模块 App 启动时向框架请求服务，拿到框架返回的模块状态后更新主页的「已激活/未激活」。

### 3.7 ProGuard / R8

若开启混淆，需加入（官方要求）：

```proguard
-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}
```

本模块当前 `minifyEnabled false`，暂不需要；一旦开启必须补上。

## 4. 兼容性权衡

迁移到 102 后：

- ✅ 用上最新 API，可用动态申请作用域、`Invoker#invokeSpecial`、按方法去优化、服务通信等能力
- ✅ 面向未来，不受经典 API 兼容层存废影响
- ❌ 只能运行在 **LSPosed 2.0+**
- ❌ 失去经典 Xposed / EdXposed / LSPatch 兼容（README 目前对外宣称支持这些）
- ❌ minSdk 由 21 提升到 26

若需要同时兼顾新旧框架，可采用**双入口**：同时保留 `assets/xposed_init` 与 `META-INF/xposed/java_init.list`，把 Hook 逻辑抽到一层与 API 无关的抽象之后，两个入口各自适配。工作量和维护成本明显更高，且共存行为需真机验证。

## 5. 迁移后测试清单

因 Hook 写法整体重写，必须逐项回归：

- [ ] 解锁会员（会员状态、失效弹窗）
- [ ] 图片保存（解密 + 导出相册）
- [ ] 解锁高清内容
- [ ] 解锁横屏旋转
- [ ] 会员金标
- [ ] 去除截屏限制
- [ ] 纯净快对（去广告）
- [ ] 红包走开 / 屏蔽提示 / 本大爷是VIP
- [ ] 不要收藏（退出解析页不弹收藏）
- [ ] 我不是新人 / 去除会员Banner
- [ ] 禁用传感器
- [ ] 解锁讲解视频（实验）
- [ ] 屏蔽启动提示
- [ ] 6.77.0 与 7.7.0 两个版本都需各测一遍（`VerSpec` 双配置）
- [ ] 模块主页「已激活」显示正确
