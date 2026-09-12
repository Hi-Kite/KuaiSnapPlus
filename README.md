<div align="center">

# 快怼+ (KuaiSnapPlus)

**对快对（com.kuaiduizuoye.scan）进行一些优化和破解**

支持框架： [LSPosed](https://github.com/LSPosed/LSPosed) ｜ [LSPatch](https://github.com/LSPosed/LSPatch)

</div>

## 关于本项目

> **KuaiSnapPlus（快怼+）是 [JiGuro/KuaiSnap](https://github.com/JiGuroLGC/KuaiSnap)（快怼）的 fork**，在原项目基础上继续维护。
>
> - **原作者**：[JiGuro](https://github.com/JiGuroLGC) —— 快怼的架构设计与绝大部分功能均由其完成，本项目是在此基础上的衍生版本
> - **本项目**：[Hi-Kite](https://github.com/Hi-Kite) 维护，新增快对 7.7.0 适配、移除完整性校验、补充若干修复等
> - 本项目遵循原项目的使用条款与免责声明，同样**仅供学习交流，严禁商用**

## 介绍

**快怼+** 是一款解锁优化 **快对** 功能的模块，旨在使软件的使用更加方便简洁。

## 说明

- 一切开发旨在学习和提升用户体验，请勿用于非法用途
- 请勿用作商业用途或大肆分发，违者严惩
- 由于个人原因，可能随时**停止更新**或**删除项目**
- 本模块完全免费，如果您支付费用才获取软件，那么您受骗了

## 详细适配

**快对** 支持 **V6.77.0 / V7.7.0** 版本，包名：**com.kuaiduizuoye.scan**
模块会自动识别作用域软件版本，无需手动选择。

主要功能：

- 解锁本地 VIP 会员、会员金标
- 解锁高清内容、横屏旋转
- 去除截屏 / 录屏限制
- 直接保存解析页图片（多选导出到相册）
- 拦截广告、屏蔽红包 / 会员 Banner / 提示横条
- 屏蔽退出解析页时的收藏弹窗
- 无水印查看（去除网页浏览图片时的平铺水印）
- 禁用传感器、解锁讲解视频（实验）等

完整开关与说明见模块内 **快对 设置页面 → 快怼+设置**。

## 框架与兼容性

本模块基于 **经典 XposedBridge API（legacy API 82）** 开发，即 `de.robv.android.xposed` 包，入口为 `assets/xposed_init`。

| 项目 | 本模块 |
|---|---|
| API 体系 | 经典 XposedBridge（`de.robv.android.xposed`） |
| API 版本 | **82**（该体系的最终版本） |
| 入口方式 | `assets/xposed_init` |
| 模块元数据 | `AndroidManifest.xml` 中的 `xposedmodule` / `xposeddescription` / `xposedscope` / `xposedminversion` |
| 最低框架 API | 54 |

**兼容的框架**：LSPosed、EdXposed、经典 Xposed、LSPatch（凡支持经典 API 的框架均可加载）。

**关于 LSPosed 的现代 API**：LSPosed 现已推出第二代 API —— **libxposed API**（`io.github.libxposed.api`，当前版本 **102**），与经典 API 是两套并行体系。本模块目前**未使用**该 API；LSPosed 2.x 仍兼容基于经典 API 的模块，因此本模块可正常运行。若日后框架移除经典 API 兼容层，本模块需要迁移，方案见 [`docs/libxposed-api102-migration.md`](docs/libxposed-api102-migration.md)。

## 开始使用

1. **下载**  
   在[发行渠道](#release)中下载最新版本。

2. **安装**  
   在设备上安装安装包，给予程序所需权限，同意 **《软件使用声明》**，然后在模块管理器中启用模块并勾选作用域。

3. **运行**  
   运行作用域软件，然后尽情享受吧！

<span id="release"></span>

## 发行渠道

| 渠道 | 地址 | 类型 |
|---|---|---|
| GitHub Releases | [Hi-Kite/KuaiSnap/releases](https://github.com/Hi-Kite/KuaiSnap/releases) | 正式版 |
| Xposed-Modules-Repo | [com.jiguro.kuaisnap](https://modules.lsposed.org/module/com.jiguro.kuaisnap) | 正式版 |

本模块发布地址仅限于上述所列出的地址，从其他非正规渠道下载到的版本或对您造成任何影响均与我们无关。

## 免责声明

```
原作者：JiGuro（以下简称"本人"或"声明者"）  
本项目"快怼+"（KuaiSnapPlus）是原作者 JiGuro 的"快怼"（KuaiSnap）项目的 fork，由 Hi-Kite 在原项目基础上继续维护。以下声明沿用原项目，同样适用于本项目。  
欢迎使用"快怼+"模块（以下简称"本软件"），这是一个对作用域软件（以下简称"Hook 软件"）进行研究的模块。在使用本软件前，请确保您已仔细阅读并完全理解并同意《软件使用声明》（以下简称"本声明"）。未成年人应在监护人的指导下阅读、理解并同意本声明后，方可使用本软件。如您不同意本声明的任何内容，请勿使用本软件。  
本软件代码系本人从互联网第三方公开渠道收集整理，分享仅供技术交流。依据《中华人民共和国计算机软件保护条例》相关规定，此软件仅用于学习和研究软件的设计思想与原理，严禁用于任何商业或非法目的。一旦学习研究目的达成，或用户决定不再用于学习研究目的，应立即将其从存储设备中彻底删除。用户需确保自身使用行为符合《中华人民共和国著作权法》、《中华人民共和国计算机软件保护条例》、《中华人民共和国网络安全法》、《中华人民共和国数据安全法》、《中华人民共和国个人信息保护法》等相关法律法规的规定，一切法律责任由使用者自行承担。  
知识产权严格受法律保护，请勿侵权。若本软件所 Hook 软件包含或基于开源软件，用户使用本软件时亦需遵守相关开源许可证的条款。本人倡导并大力支持正版软件，正版软件的使用不仅能确保良好的用户体验和稳定的性能，更是对软件开发者创新和努力的尊重与支持。如果您发现 Hook 软件对您有帮助或您喜欢它，请积极支持正版。  
在此，特别强调，本人分享本软件纯粹是为了学习交流之目的，不带有任何盈利意图。同时，用户应充分认识到，由于本软件代码来源的第三方属性及本人能力的局限性，本人无法对本软件的安全性（包括但不限于是否存在计算机病毒、恶意代码、后门程序、安全漏洞或侵犯隐私的功能）、合法性（如版权状态）提供任何形式的保证或担保。在任何情况下，声明者均不对因下载、安装、使用、无法使用或依赖本软件所导致的任何直接、间接、附带、特殊、惩罚性或后果性的损害（包括但不限于数据丢失、系统损坏、业务中断、利润损失、隐私泄露、法律纠纷等）承担任何责任，无论该等责任是基于合同、侵权（包括过失）、严格责任或其他法律理论产生，也无论声明者是否事先被告知该等损害的可能性。本人强烈建议用户弃用模块，并通过软件开发者官方渠道获取正版软件以确保安全性和合法性。本人承诺，本软件的分享过程中未主动植入任何计算机病毒、后门程序，也未故意设置任何用于侵犯用户隐私的功能。若用户发现本软件有此类问题，请务必及时联系本人，本人将立即采取删除等措施，坚决阻止本软件的进一步传播。  
在首次运行本软件时，用户将被强制要求通过本软件提供的交互界面（如弹出窗口）完整阅读本声明内容，并需主动选择"同意"或"我已理解"等类似选项，明确同意本声明内容。用户一旦运行或继续使用本软件，即视为已仔细阅读、完全理解且同意接受本声明的全部内容，并自愿承担因下载、安装、使用本软件所产生的一切法律责任和后果。请务必合法使用本软件，共同维护良好的网络环境和法律秩序。  
本声明的最终解释权归声明者所有。
```

## 鸣谢

| 名称 | 链接 | 详情 |
|---|---|---|
| JiGuro | [JiGuro - GitHub](https://github.com/JiGuroLGC) | 原项目 KuaiSnap 作者 |
| 咫尺天涯间. | [MT论坛](https://bbs.binmt.cc/home.php?mod=space&uid=103635&do=profile) | 提供部分 VIP 破解方法 |
| dotcog | [MT论坛](https://bbs.binmt.cc/home.php?mod=space&uid=148156&do=profile) | 提供去广告部分代码 |
| AIDE Pro | —— | 开发打包软件 |
| Android IDE | [AndroidIDEOfficial/AndroidIDE](https://github.com/AndroidIDEOfficial/AndroidIDE) | 开发打包软件 |
| 一言 | [hitokoto.cn](https://hitokoto.cn/) | 提供诗句支持 |

## 支持我们

问题反馈：[GitHub Issues](https://github.com/Hi-Kite/KuaiSnap/issues)

如果这个项目对你有帮助，欢迎 Star 支持。

版权所有 © 2025 JiGuro（原项目 KuaiSnap）  
KuaiSnapPlus（快怼+）由 [Hi-Kite](https://github.com/Hi-Kite) 维护，基于原项目 fork 而来
