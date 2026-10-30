from __future__ import annotations

from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.style import WD_STYLE_TYPE
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK, WD_LINE_SPACING
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor


ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = ROOT / "store" / "copyright"
SOFTWARE_NAME = "Mealory饮食记录软件"
VERSION = "V1.0"
MANUAL_PATH = OUTPUT_DIR / f"{SOFTWARE_NAME}{VERSION}用户操作手册.docx"
SOURCE_PATH = OUTPUT_DIR / f"{SOFTWARE_NAME}{VERSION}源程序.docx"
LOGO_PATH = ROOT / "store" / "oppo" / "assets" / "mealory-oppo-icon-512.png"


MANUAL_PAGES: list[tuple[str, list[str]]] = [
    (
        "第一章 软件概述",
        [
            "Mealory 是一款运行在 Android 手机上的饮食记录软件。",
            "软件简称为 Mealory，登记版本为 V1.0。",
            "本手册说明安装、记录、浏览、备份和数据管理方法。",
            "软件面向希望长期保存饮食回忆的个人用户。",
            "软件不提供热量计算、营养分析或打卡排名。",
            "软件不要求注册账号，也不要求登录。",
            "软件不含广告、付费项目或社交功能。",
            "软件可在 Android 8.0 及以上版本运行。",
            "建议使用具备正常存储空间的 Android 智能手机。",
            "全部餐食记录默认保存在应用私有目录。",
            "餐食照片默认保存在应用私有图片目录。",
            "结构化记录由本地数据库管理。",
            "主题和照片质量等设置保存在本地。",
            "软件没有网络访问能力，离线即可使用。",
            "软件不会把记录上传到服务器。",
            "软件不会读取通讯录或定位信息。",
            "软件不会申请系统相机运行时权限。",
            "拍照功能由用户主动打开系统相机完成。",
            "相册选图由 Android 系统照片选择器完成。",
            "软件只能读取用户主动选择的照片。",
            "备份文件由系统文件选择器保存或打开。",
            "完整备份采用可解压的 ZIP 文件格式。",
            "备份中包含开放的 JSON 数据和 JPG 照片。",
            "用户可通过时间线浏览全部有效记录。",
            "用户可按饭名、备注、地点和人物进行搜索。",
            "用户可通过月历、年历和日期详情回顾记录。",
            "用户可查看过去的今天、随机一顿和年度回忆。",
            "误删记录可在最近删除中保留三十天。",
            "卸载软件或清除数据前应先导出完整备份。",
            "本手册中的应用名称 Mealory 指本软件简称。",
        ],
    ),
    (
        "第二章 安装与首次启动",
        [
            "从可信应用商店下载 Mealory 安装包。",
            "在系统安装页面确认应用名称和来源。",
            "点击安装并等待系统完成校验。",
            "安装完成后点击系统页面中的打开按钮。",
            "也可以从手机桌面点击 Mealory 图标启动。",
            "首次启动时软件先显示品牌启动页。",
            "启动页结束后进入首次使用说明。",
            "首次使用说明介绍记录与回忆的主要用途。",
            "点击开始记录进入隐私政策确认页面。",
            "隐私政策说明本地数据、照片和备份的处理方式。",
            "请完整阅读隐私政策后再作选择。",
            "点击同意并继续可进入软件主界面。",
            "同意状态会保存在本机设置中。",
            "再次启动时通常不会重复显示同一版本政策。",
            "点击不同意并退出会关闭当前软件界面。",
            "不同意不会创建餐食记录或读取用户照片。",
            "隐私政策版本更新时可能需要重新确认。",
            "用户可在设置页面随时再次查看完整政策。",
            "主界面底部包含首页、日历、回忆和我的。",
            "首次进入首页时会显示空状态提示。",
            "首页右下角的按钮用于新增餐食记录。",
            "软件不会在首次启动时要求注册账号。",
            "软件不会在首次启动时请求联网权限。",
            "软件不会自动扫描系统相册。",
            "软件不会自动启动相机或录音设备。",
            "系统可能显示兼容性或电池管理提示。",
            "此类提示由手机系统提供，不影响本地记录。",
            "若启动失败，可先从最近任务关闭软件后重试。",
            "若安装包损坏，应从可信应用商店重新下载。",
            "完成上述步骤后即可开始记录第一顿饭。",
        ],
    ),
    (
        "第三章 主界面与导航",
        [
            "首页用于查看按日期和时间排序的餐食时间线。",
            "首页顶部显示 Mealory 名称和本地搜索框。",
            "搜索不会把关键词发送到网络。",
            "首页中的记录按今天、昨天或具体日期分组。",
            "有照片的记录优先显示封面照片。",
            "没有照片的记录仍会显示文字信息。",
            "餐食卡片可显示餐次、时间和饭名。",
            "卡片还可显示地点与同行人物摘要。",
            "点击餐食卡片进入该记录的详情页面。",
            "日历页用于按月、按年和按日期浏览。",
            "月视图显示当月日期和每天的记录数量。",
            "年视图显示十二个月的记录概览。",
            "回忆页用于重新发现较早的饮食记录。",
            "回忆页显示已记录天数和有效餐食总数。",
            "过去的今天会优先显示去年同日记录。",
            "随机一顿会从较早的有效记录中抽取。",
            "过去的这个月按年份汇总同月记录。",
            "我的页面用于查看个人数据概览。",
            "我的页面不要求设置头像或个人资料。",
            "人物入口用于管理同行者标签。",
            "地点入口用于管理用餐地点标签。",
            "最近删除入口用于恢复或永久删除记录。",
            "数据与备份入口用于导出和恢复完整备份。",
            "设置入口用于调整主题和照片质量。",
            "底部导航始终提供四个主要页面入口。",
            "点击当前页对应的导航项不会修改数据。",
            "返回键用于退出详情页或返回上一级页面。",
            "系统手势导航和三键导航均可正常使用。",
            "界面会跟随系统或用户选择的明暗主题。",
            "所有主要操作均可在离线状态完成。",
        ],
    ),
    (
        "第四章 新增餐食记录",
        [
            "在首页点击右下角的记录一顿饭按钮。",
            "软件会显示拍照、从相册选择和无照片补记。",
            "选择拍照后，Android 系统相机会被打开。",
            "在系统相机中完成构图并点击拍摄。",
            "确认照片后，系统把结果返回给 Mealory。",
            "若在相机中取消，软件不会创建餐食记录。",
            "Mealory 本身不申请相机运行时权限。",
            "选择从相册选择会打开系统照片选择器。",
            "用户可主动选择一张或多张餐食照片。",
            "一顿饭最多可保存九张照片。",
            "系统只授权软件读取用户选中的照片。",
            "若取消照片选择，原有数据不会发生变化。",
            "选择无照片补记可直接进入记录编辑页面。",
            "无照片记录仍可保存完整日期和文字信息。",
            "导入照片后软件会读取可用的拍摄时间信息。",
            "照片包含原始拍摄时间时会建议相应日期。",
            "用户可以修改软件建议的日期和时间。",
            "照片方向信息用于保证显示方向正确。",
            "导入照片会按设置中的质量档位处理。",
            "标准质量适合多数日常记录。",
            "高质量会保留更大的照片尺寸并占用更多空间。",
            "处理后的照片保存在应用私有目录。",
            "原相册中的照片不会因导入而被删除。",
            "用户可在编辑页面继续添加照片。",
            "用户可从当前记录中移除尚未保存的照片。",
            "用户可调整多张照片的显示顺序。",
            "用户可选择其中一张作为封面。",
            "退出未保存页面时不会产生完整餐食记录。",
            "存储空间不足时软件会显示可理解的错误提示。",
            "确认照片后继续填写餐食信息并保存。",
        ],
    ),
    (
        "第五章 编辑与保存记录",
        [
            "记录编辑页面显示餐次、日期和时间。",
            "餐次可根据时间自动判断，也可以手动选择。",
            "软件支持早餐、午餐、晚餐和加餐。",
            "日期字段用于表示实际用餐日期。",
            "时间字段用于表示实际用餐时间。",
            "饭名字段用于填写本顿饭的简短名称。",
            "饭名可以留空，不影响记录保存。",
            "地点字段用于填写或选择用餐地点。",
            "地点可以留空，软件不会读取定位信息。",
            "人物字段用于选择或新增同行者标签。",
            "一顿饭可以关联多位同行者。",
            "人物可以留空，软件不会读取通讯录。",
            "备注字段用于保存自由文字。",
            "备注可以留空，也可以记录当时的感受。",
            "输入内容具有合理长度限制。",
            "过长输入会被截取到允许的最大长度。",
            "最近使用的地点会显示为快捷选项。",
            "已有的人物会显示为可多选标签。",
            "重复名称会尽量复用已有标签。",
            "名称首尾和连续空白会被规范化。",
            "保存前可以再次检查照片顺序和封面。",
            "点击保存后软件开始处理照片与数据库记录。",
            "保存成功后会返回主界面。",
            "首页会立即显示新保存的餐食记录。",
            "保存成功会显示这一顿记下了的提示。",
            "保存失败时软件会说明可能的原因。",
            "数据库写入失败时会清理未完成的临时照片。",
            "相机临时文件不会长期占用存储空间。",
            "保存后可从餐食详情继续修改文字信息。",
            "建议在重要记录完成后定期导出完整备份。",
        ],
    ),
    (
        "第六章 首页搜索与餐食详情",
        [
            "首页默认按实际日期和时间倒序显示记录。",
            "较新的记录显示在时间线更靠前的位置。",
            "大量记录会分批读取，避免一次加载全部数据。",
            "向下滚动可继续浏览较早的餐食记录。",
            "点击搜索框后可以输入本地搜索关键词。",
            "搜索范围包括饭名、备注、地点和人物。",
            "输入关键词后结果会在本机数据库中筛选。",
            "特殊符号会按普通文字处理。",
            "已放入最近删除的记录不会出现在搜索结果中。",
            "没有匹配结果时软件会显示空结果提示。",
            "清空关键词即可恢复完整时间线。",
            "点击任意餐食卡片可进入详情页面。",
            "详情页显示日期、时间、餐次和文字信息。",
            "有多张照片时可左右滑动浏览。",
            "封面照片优先作为详情页首张照片。",
            "用户可在详情页切换封面照片。",
            "点击编辑可修改日期、时间和餐次。",
            "用户也可修改饭名、地点、人物和备注。",
            "编辑保存后首页和相关统计会自动更新。",
            "点击删除会先显示二次确认。",
            "普通删除会把记录移动到最近删除。",
            "普通删除不会立即永久清除照片。",
            "删除后的记录会立即离开首页和搜索结果。",
            "删除后的记录也不会参与日历和回忆。",
            "用户可在三十天内从最近删除恢复。",
            "详情页面的返回按钮用于回到上一页面。",
            "照片缺失或损坏时软件会显示占位图。",
            "单张异常照片不会阻止其他记录正常显示。",
            "浏览记录不会修改照片或数据库内容。",
            "所有搜索与详情操作均不需要网络连接。",
        ],
    ),
    (
        "第七章 日历与日期回顾",
        [
            "点击底部日历进入日历页面。",
            "日历页面默认显示当前月份。",
            "页面顶部可在月视图和年视图之间切换。",
            "月视图按周排列当前月份的日期。",
            "今天会使用清晰但克制的样式标记。",
            "有记录的日期会显示餐食数量。",
            "有照片时日期格可显示当天代表照片。",
            "没有照片的日期仍会显示记录数量。",
            "点击上一个周期可查看上个月。",
            "点击下一个周期可查看下个月。",
            "点击有记录的日期会显示当天预览。",
            "当天预览列出该日期的全部有效记录。",
            "点击预览中的餐食可直接进入详情。",
            "也可以继续进入完整日期详情页面。",
            "日期详情按当天时间顺序显示餐食。",
            "日期详情可显示照片、饭名、地点和人物。",
            "从日期详情点击餐食也可进入完整详情。",
            "年视图显示目标年份的十二个月。",
            "每个月会显示该月有效记录数量。",
            "有照片的月份会显示代表照片。",
            "点击月份会返回该月的月视图。",
            "年视图支持切换到上一年或下一年。",
            "跨年记录会按照各自的实际日期归档。",
            "放入最近删除的记录不会显示在日历中。",
            "恢复记录后对应日期会重新出现。",
            "日历只读取展示所需的轻量数据。",
            "打开日期详情时才读取该日完整关系数据。",
            "日历不会生成不存在的记录或照片。",
            "日期和月份统计全部来自本地数据库。",
            "日历浏览全过程均不需要网络连接。",
        ],
    ),
    (
        "第八章 回忆与年度回忆",
        [
            "点击底部回忆进入回忆页面。",
            "页面显示与 Mealory 相伴的真实天数。",
            "页面同时显示当前有效餐食总数。",
            "统计不采用连续打卡或漏记惩罚。",
            "过去的今天优先寻找去年同一天的记录。",
            "没有精确同日记录时只在前后三天内查找。",
            "回忆卡片会显示真实的距今天数。",
            "点击回忆卡片可进入相应餐食详情。",
            "随机一顿优先从三十天以前的记录中选择。",
            "点击再来一顿会尽量避开当前记录。",
            "随机结果仅来自当前未删除的真实数据。",
            "过去的这个月会按历史年份分组。",
            "每个年份显示同一月份的有效记录数量。",
            "每个年份最多显示三张代表照片。",
            "无历史记录时页面会如实显示空状态。",
            "存在年度数据时会显示我的某年入口。",
            "点击年度入口进入对应年份的年度回忆。",
            "年度页显示总记录顿数和不同记录日数。",
            "年度页显示该年的第一顿和最后一顿。",
            "全年只有一顿时会合并为一张真实卡片。",
            "里程碑依据全部有效记录的时间顺序计算。",
            "达到相应数量时可显示第一百顿等里程碑。",
            "常见同行者至少出现两次才会展示。",
            "常去地点至少出现两次才会展示。",
            "并列第一的人物或地点会完整显示。",
            "春夏秋冬拼图只使用该年真实餐食照片。",
            "点击拼图中的照片可返回餐食详情。",
            "放入最近删除的记录不会参与回忆计算。",
            "恢复记录后相关回忆和统计会自动更新。",
            "回忆功能全部在本机数据库中完成。",
        ],
    ),
    (
        "第九章 人物地点与最近删除",
        [
            "在我的页面点击人物进入人物列表。",
            "人物是用户自行创建的本地文字标签。",
            "软件不会从系统通讯录读取人物信息。",
            "用户可以创建、重命名或删除人物标签。",
            "人物详情显示关联餐食数量。",
            "人物详情显示首次和最近一次记录日期。",
            "人物详情按时间倒序显示关联餐食。",
            "删除人物只移除标签关系，不删除餐食。",
            "在我的页面点击地点进入地点列表。",
            "地点是用户自行创建的本地文字标签。",
            "软件不会读取手机定位或地图信息。",
            "用户可以创建、重命名或删除地点标签。",
            "地点详情显示关联餐食数量。",
            "地点详情显示首次和最近一次记录日期。",
            "地点详情按时间倒序显示关联餐食。",
            "删除地点只清除关联地点，不删除餐食。",
            "记录页面会提供最近使用地点的快捷选择。",
            "人物和地点的同名输入会尽量复用原标签。",
            "在我的页面点击最近删除查看已删除记录。",
            "列表按删除时间倒序显示。",
            "每条记录会显示剩余可恢复天数。",
            "点击恢复可把记录放回原有日期。",
            "恢复后首页、搜索、日历和回忆会重新显示。",
            "点击永久删除会先显示二次确认。",
            "永久删除会移除数据库记录和私有照片。",
            "永久删除后无法通过软件恢复。",
            "超过三十天的记录会由后台任务安全清理。",
            "后台清理只处理 Mealory 自己的私有文件。",
            "软件会校验照片路径，避免触碰其他文件。",
            "重要记录删除前建议先导出完整备份。",
        ],
    ),
    (
        "第十章 完整备份与恢复",
        [
            "在我的页面点击数据与备份。",
            "页面显示导出完整备份和从备份恢复。",
            "点击导出完整备份会打开系统保存窗口。",
            "建议使用默认的 Mealory Backup 日期文件名。",
            "用户可在系统窗口选择实际保存位置。",
            "软件不会申请访问整个存储空间的权限。",
            "确认位置后软件开始读取本地记录和照片。",
            "导出的 ZIP 包含版本化的 manifest 文件。",
            "ZIP 包含餐食、照片、人物、地点和关系数据。",
            "ZIP 包含用户设置和全部可用的 JPG 照片。",
            "每张备份照片都记录文件大小和 SHA 256。",
            "导出成功后页面会更新最近备份时间。",
            "照片缺失时软件会停止导出并提示问题。",
            "导出失败不会修改原有数据库和照片。",
            "建议把重要备份复制到另一安全位置。",
            "点击从备份恢复会打开系统文件选择器。",
            "请选择由 Mealory 导出的完整 ZIP 备份。",
            "软件先在独立临时目录校验备份。",
            "校验包括格式版本、数据关系和文件路径。",
            "校验还包括照片大小和 SHA 256 摘要。",
            "异常路径、损坏照片或不完整数据会被拒绝。",
            "校验通过后软件显示备份时间和数据数量。",
            "恢复前必须再次确认完整替换操作。",
            "完整替换会移除当前数据并写入备份内容。",
            "普通恢复失败时软件会回滚到操作前状态。",
            "恢复成功后首页会显示备份中的记录。",
            "备份文件可以用常见解压工具查看。",
            "不要手工修改 ZIP 后再尝试恢复。",
            "卸载软件或更换手机前必须先导出备份。",
            "恢复与导出全过程均由用户主动选择文件。",
        ],
    ),
    (
        "第十一章 设置与隐私",
        [
            "在我的页面点击设置进入设置页面。",
            "主题设置提供跟随系统、浅色和深色。",
            "选择主题后界面会立即更新。",
            "主题选择会保存在本机设置中。",
            "照片质量提供标准和高质量两档。",
            "标准质量最长边约为二千五百六十像素。",
            "高质量最长边约为四千零九十六像素。",
            "高质量照片更清晰，也会占用更多空间。",
            "质量设置只影响之后导入的新照片。",
            "设置页显示当前照片数量和占用空间。",
            "设置页显示最近一次成功备份时间。",
            "点击隐私政策可再次查看完整政策。",
            "隐私政策说明数据处理范围和用户选择。",
            "应用版本信息用于核对当前安装版本。",
            "Mealory V1.0 对应商店显示版本 1.0.0。",
            "软件 Manifest 不声明网络访问权限。",
            "软件不声明定位、通讯录或短信权限。",
            "软件不声明麦克风或相机运行时权限。",
            "软件不声明全相册读取权限。",
            "系统相机仅在用户点击拍照后打开。",
            "系统照片选择器仅在用户点击选图后打开。",
            "系统文件选择器仅在备份操作时打开。",
            "照片拍摄时间只用于建议记录日期和时间。",
            "照片方向信息只用于正确显示照片。",
            "软件不会创建跨设备账号或云端资料。",
            "软件不会接入广告、统计或推送服务。",
            "删除或恢复操作只处理本机 Mealory 数据。",
            "用户可通过完整备份迁移自己的数据。",
            "卸载软件会由 Android 删除应用私有数据。",
            "卸载前应确认有效备份已经保存成功。",
        ],
    ),
    (
        "第十二章 常见问题与安全操作",
        [
            "软件无法启动时先从最近任务中完全关闭后重试。",
            "仍无法启动时可重新启动手机后再次打开。",
            "不要在未备份时清除 Mealory 应用数据。",
            "不要在未备份时直接卸载软件。",
            "拍照未返回时检查系统相机是否正常工作。",
            "在系统相机中取消不会产生餐食记录。",
            "照片选择器为空时检查系统相册中是否有照片。",
            "软件无法读取未被用户主动选择的照片。",
            "照片显示占位图时原文件可能已损坏或缺失。",
            "可从有效完整备份恢复缺失的数据和照片。",
            "保存失败时先检查手机剩余存储空间。",
            "释放空间后可重新执行记录或备份操作。",
            "搜索无结果时可缩短关键词或检查输入内容。",
            "删除的记录应在最近删除页面查找。",
            "超过三十天或永久删除的记录不能直接恢复。",
            "备份无法打开时确认文件是完整 ZIP。",
            "零字节或未完成的文件不是有效备份。",
            "恢复被拒绝表示文件校验未通过。",
            "不要为了恢复而关闭安全校验或修改程序。",
            "恢复前先确认当前数据是否需要另行备份。",
            "恢复操作会用备份内容完整替换当前数据。",
            "更换手机时先在旧手机导出完整备份。",
            "再在新手机安装相同或兼容版本的 Mealory。",
            "最后在新手机选择备份文件并确认恢复。",
            "签名不同的测试版和正式版可能无法直接覆盖。",
            "遇到覆盖失败时应先确认有效备份已经保存。",
            "不要向任何人提供正式签名密钥或密码。",
            "不要把含个人餐食照片的备份公开上传。",
            "需要帮助时可通过商店页面公布的支持邮箱联系。",
            "以上操作均以保护本地数据完整性为优先。",
        ],
    ),
]


def set_run_font(run, name: str, size: float, bold: bool | None = None) -> None:
    run.font.name = name
    run.font.size = Pt(size)
    run._element.get_or_add_rPr().rFonts.set(qn("w:ascii"), name)
    run._element.get_or_add_rPr().rFonts.set(qn("w:hAnsi"), name)
    run._element.get_or_add_rPr().rFonts.set(qn("w:eastAsia"), name)
    if bold is not None:
        run.bold = bold


def set_a4(section) -> None:
    section.page_width = Cm(21.0)
    section.page_height = Cm(29.7)


def add_page_field(paragraph) -> None:
    run = paragraph.add_run()
    begin = OxmlElement("w:fldChar")
    begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = " PAGE "
    separate = OxmlElement("w:fldChar")
    separate.set(qn("w:fldCharType"), "separate")
    text = OxmlElement("w:t")
    text.text = "1"
    end = OxmlElement("w:fldChar")
    end.set(qn("w:fldCharType"), "end")
    run._r.extend([begin, instr, separate, text, end])


def add_common_header_footer(doc: Document, title: str) -> None:
    section = doc.sections[0]
    header = section.header
    p = header.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run(title)
    set_run_font(r, "Microsoft YaHei", 9)
    footer = section.footer
    p = footer.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    prefix = p.add_run(f"{SOFTWARE_NAME} {VERSION}  第 ")
    set_run_font(prefix, "Microsoft YaHei", 8)
    add_page_field(p)
    suffix = p.add_run(" 页")
    set_run_font(suffix, "Microsoft YaHei", 8)


def build_manual() -> None:
    doc = Document()
    section = doc.sections[0]
    set_a4(section)
    section.top_margin = Cm(1.65)
    section.bottom_margin = Cm(1.65)
    section.left_margin = Cm(2.0)
    section.right_margin = Cm(2.0)
    section.header_distance = Cm(0.75)
    section.footer_distance = Cm(0.75)

    styles = doc.styles
    normal = styles["Normal"]
    normal.font.name = "Microsoft YaHei"
    normal.font.size = Pt(10)
    normal._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")
    for style_name, size in [("Title", 24), ("Heading 1", 16), ("Heading 2", 13)]:
        style = styles[style_name]
        style.font.name = "Microsoft YaHei"
        style.font.size = Pt(size)
        style.font.color.rgb = RGBColor(0, 0, 0)
        style._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")

    add_common_header_footer(doc, f"{SOFTWARE_NAME} {VERSION} 用户操作手册")

    for page_index, (heading, lines) in enumerate(MANUAL_PAGES):
        if len(lines) < 30:
            raise ValueError(f"{heading} has only {len(lines)} lines")
        if page_index == 0:
            top = doc.add_paragraph()
            top.alignment = WD_ALIGN_PARAGRAPH.CENTER
            top.paragraph_format.space_after = Pt(4)
            if LOGO_PATH.exists():
                top.add_run().add_picture(str(LOGO_PATH), width=Cm(1.8))
            # The built-in Word Title style carries a theme border in some
            # installations. Use a plain paragraph so the registration copy
            # renders consistently without a decorative blue rule.
            title = doc.add_paragraph()
            title.alignment = WD_ALIGN_PARAGRAPH.CENTER
            title.paragraph_format.space_after = Pt(2)
            run = title.add_run(f"{SOFTWARE_NAME}用户操作手册")
            set_run_font(run, "Microsoft YaHei", 20, True)
            subtitle = doc.add_paragraph()
            subtitle.alignment = WD_ALIGN_PARAGRAPH.CENTER
            subtitle.paragraph_format.space_after = Pt(8)
            run = subtitle.add_run(VERSION)
            set_run_font(run, "Microsoft YaHei", 11)

        h = doc.add_paragraph(style="Heading 1")
        h.paragraph_format.space_before = Pt(0)
        h.paragraph_format.space_after = Pt(5)
        h.paragraph_format.keep_with_next = True
        run = h.add_run(heading)
        set_run_font(run, "Microsoft YaHei", 14, True)

        for line in lines:
            p = doc.add_paragraph()
            p.paragraph_format.left_indent = Cm(0.2)
            p.paragraph_format.first_line_indent = Cm(0)
            p.paragraph_format.space_before = Pt(0)
            p.paragraph_format.space_after = Pt(0)
            p.paragraph_format.line_spacing_rule = WD_LINE_SPACING.EXACTLY
            p.paragraph_format.line_spacing = Pt(13.8)
            r = p.add_run(line)
            set_run_font(r, "Microsoft YaHei", 9.5)

        if page_index < len(MANUAL_PAGES) - 1:
            doc.add_page_break()

    doc.core_properties.title = f"{SOFTWARE_NAME}{VERSION}用户操作手册"
    doc.core_properties.subject = "计算机软件著作权登记文档鉴别材料"
    doc.core_properties.author = "Mealory"
    doc.core_properties.keywords = "Mealory, 软件著作权, 用户操作手册"
    doc.save(MANUAL_PATH)


def collect_nonblank_source_lines() -> list[tuple[str, int, str]]:
    source_root = ROOT / "app" / "src" / "main"
    files = sorted(source_root.rglob("*.kt"), key=lambda p: p.as_posix().lower())
    result: list[tuple[str, int, str]] = []
    for path in files:
        relative = path.relative_to(ROOT).as_posix()
        for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
            if line.strip():
                result.append((relative, number, line.rstrip()))
    return result


def build_source() -> None:
    source_lines = collect_nonblank_source_lines()
    if len(source_lines) < 3000:
        raise ValueError(f"Only {len(source_lines)} nonblank source lines found")
    selected = source_lines[:1500] + source_lines[-1500:]

    doc = Document()
    section = doc.sections[0]
    set_a4(section)
    section.top_margin = Cm(1.35)
    section.bottom_margin = Cm(1.35)
    section.left_margin = Cm(1.2)
    section.right_margin = Cm(1.2)
    section.header_distance = Cm(0.55)
    section.footer_distance = Cm(0.55)

    normal = doc.styles["Normal"]
    normal.font.name = "Consolas"
    normal.font.size = Pt(5.5)
    normal._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")

    add_common_header_footer(doc, f"{SOFTWARE_NAME} {VERSION} 源程序")

    for index, (relative, original_number, text) in enumerate(selected):
        global_number = index + 1 if index < 1500 else len(source_lines) - 1500 + (index - 1500) + 1
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(0)
        p.paragraph_format.line_spacing_rule = WD_LINE_SPACING.EXACTLY
        p.paragraph_format.line_spacing = Pt(9.2)
        p.paragraph_format.keep_together = True
        display = f"{global_number:05d}  {text}"
        r = p.add_run(display)
        set_run_font(r, "Consolas", 5.5)
        r._element.get_or_add_rPr().rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")

        is_page_end = (index + 1) % 50 == 0
        if is_page_end and index < len(selected) - 1:
            p.add_run().add_break(WD_BREAK.PAGE)

    doc.core_properties.title = f"{SOFTWARE_NAME}{VERSION}源程序"
    doc.core_properties.subject = "计算机软件著作权登记源程序鉴别材料"
    doc.core_properties.author = "Mealory"
    doc.core_properties.keywords = "Mealory, 软件著作权, Kotlin, 源程序"
    doc.save(SOURCE_PATH)

    manifest = OUTPUT_DIR / "SOURCE_SELECTION.txt"
    first = selected[0]
    first_end = selected[1499]
    last = selected[1500]
    last_end = selected[-1]
    manifest.write_text(
        "\n".join(
            [
                f"全部非空 Kotlin 源程序行数：{len(source_lines)}",
                "源程序文档共 60 页，每页 50 行。",
                f"前 30 页：第 1–1500 行，从 {first[0]}:{first[1]} 到 {first_end[0]}:{first_end[1]}",
                f"后 30 页：第 {len(source_lines)-1499}–{len(source_lines)} 行，从 {last[0]}:{last[1]} 到 {last_end[0]}:{last_end[1]}",
                "空白行未计入每页 50 行；源程序正文内容和顺序未改写。",
            ]
        ),
        encoding="utf-8",
    )


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    build_manual()
    build_source()
    print(MANUAL_PATH)
    print(SOURCE_PATH)


if __name__ == "__main__":
    main()
