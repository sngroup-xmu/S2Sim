import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import matplotlib.lines as mlines

# 读取 Excel 文件
df1 = pd.read_excel('/home/gaohan/Scalpel-batfish/eval_data_nsdi26/ipran-3k.xlsx')


# 提取数据
sim_time = df1['Sim Time']

sim_time = sim_time / 1000
# 计算数据的 CDF
sorted_data1 = np.sort(sim_time)
cumulative1 = np.arange(len(sorted_data1)) / float(len(sorted_data1))


fig, ax = plt.subplots(figsize=(8, 5))

# 绘制 CDF 曲线
ax.plot(sorted_data1, cumulative1,color='blue')
ax.scatter(sorted_data1, cumulative1, color='blue', s=30, marker='o')


# 计算九十分位数
quantile_90_1 = np.percentile(sim_time, 90)

# 横向参考线（y 方向网格）
ax.yaxis.grid(True, linestyle='--', linewidth=1.2)
ax.xaxis.grid(False)

# 字体大小和粗细
label_font = {'fontsize': 18, 'fontweight': 'bold'}
tick_font = {'fontsize': 13, 'fontweight': 'bold'}

# 坐标轴标签
ax.set_xlabel('Simulation Time (s)', **label_font)
ax.set_ylabel('CDF', **label_font)
# ax.set_title('Request vs Time', fontsize=16, fontweight='bold')

# 坐标轴刻度字体
ax.tick_params(axis='both', labelsize=tick_font['fontsize'], width=1.8)
for label in ax.get_xticklabels() + ax.get_yticklabels():
    label.set_fontweight('bold')

# 设置坐标轴轮廓线粗度
for spine in ax.spines.values():
    spine.set_linewidth(2)

# 在图中绘制九十分位线
ax.axvline(quantile_90_1, color='red', linestyle='dashed', linewidth=1.5, label='90th Percentile Fattree')

# # 添加标签和标题
# plt.xlabel('Simulation Time(ms)')
# plt.ylabel('CDF')

# 显示图例
# legend_line = mlines.Line2D([], [], color='blue', marker='o', markersize=6)
# plt.legend(handles=[legend_line])


plt.savefig('/home/gaohan/Scalpel-batfish/eval_data_nsdi26/cdf.pdf')


# 最后关闭图表
# plt.close()
