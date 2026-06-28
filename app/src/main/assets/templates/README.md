# 模板图片说明

将游戏界面中的按钮、图标等元素截图后，按以下文件名放入本目录（PNG格式）。

**制作模板图技巧：**
1. 在游戏中截取对应按钮/图标的清晰截图
2. 尽量裁剪到刚好包含元素本身，不要有太多多余背景
3. 建议尺寸 48x48 ~ 128x128 像素（根据实际游戏分辨率调整）
4. 保持 PNG 透明背景更佳（如果是纯色按钮也可以不透明）
5. 匹配阈值默认 0.8，可在代码中调整

## 资源收取模块 (ResourceModule)
| 文件名 | 说明 |
|--------|------|
| `collect_btn.png` | 资源建筑上方的"收取"按钮图标 |
| `claim_all.png` | 右下角资源面板的"全部收取"按钮 |
| `offline_reward.png` | 离线收益弹窗标识 |
| `claim_btn.png` | 通用"领取"按钮 |
| `close_btn.png` | 关闭按钮 (X 图标) |

## 邮件模块 (MailModule)
| 文件名 | 说明 |
|--------|------|
| `mail_icon.png` | 主界面的邮件图标入口 |
| `claim_all_mail.png` | 邮件界面的"全部领取"按钮 |
| `mail_close.png` | 邮件界面的关闭按钮 |
| `confirm_btn.png` | 确认/确定按钮 |

## 任务模块 (TaskModule)
| 文件名 | 说明 |
|--------|------|
| `task_icon.png` | 主界面的任务图标入口 |
| `task_claim.png` | 任务列表中的"领取"按钮 |
| `chest_claim.png` | 活跃度宝箱的领取按钮 |
| `task_close.png` | 任务面板关闭按钮 |
| `tab_daily.png` | 每日任务标签页 |
| `tab_active.png` | 活跃度标签页 |

## 野外采集模块 (GatherModule)
| 文件名 | 说明 |
|--------|------|
| `map_icon.png` | 世界地图按钮 |
| `wood_icon.png` | 木材资源点图标 |
| `iron_icon.png` | 铁矿资源点图标 |
| `oil_icon.png` | 原油资源点图标 |
| `food_icon.png` | 粮食资源点图标 |
| `gather_btn.png` | "采集"按钮 |
| `march_btn.png` | "出征/行军"按钮 |
| `recall_btn.png` | "召回"按钮 |
| `confirm_march.png` | "确认出征"按钮 |
| `back_city.png` | "返回基地/回城"按钮 |
| `troop_full.png` | 部队满载返回提示图标 |

## 通用
- 缺哪个模板，对应功能会自动跳过，不会崩溃
- 可以逐步添加，先做通核心功能再加其他
- 模板文件大小控制在 200KB 以内
