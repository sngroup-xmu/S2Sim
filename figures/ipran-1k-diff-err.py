import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

# 读取 Excel 文件
df = pd.read_excel('/home/gaohan/Scalpel-batfish/eval_data_nsdi26/ipran-1k.xlsx')

# 提取数据
# Errors = df['Errors'][:8]#到fattree18
Errors = df['Errors']
time_1 = df['one'] / 1000
time_2 = df['two'] / 1000
time_3 = df['three'] / 1000
time_4 = df['four'] / 1000
time_5 = df['five'] / 1000
# scalpel_time2 = df['Second Simulation Time']

# 设置柱状图宽度
bar_width = 0.15

# 设置颜色和填充样式
color_nonFailure = '#c5defa'
color_time = '#c5defa'
# color_scalpel2 = '#F0A19A'
# hatch_pattern1 = '--'  # 斜杠填充
# hatch_pattern2 = "xx"
# hatch_pattern3 = "\\\\"


# 计算平均值
row2 = [91481, 98436, 101260, 104379, 113743]
row3 = [101397, 105525, 107483, 109699, 114397]
row4 = [102283, 104304, 105371, 105870, 113200]

value1 = np.mean(row2) / 1000
value2 = np.mean(row3) / 1000
value3 = np.mean(row4) / 1000

values = [value1,value2,value3]
tmp = [0,0,0]

# 生成两个柱状图
fig, ax = plt.subplots()
# time1Bar = ax.bar(np.arange(len(Errors)), time_1, width=bar_width, color=color_time,linewidth=1,edgecolor='black')
# time2Bar = ax.bar(np.arange(len(Errors)) + bar_width, time_2, width=bar_width, color=color_time,linewidth=1,edgecolor='black')
# time3Bar = ax.bar(np.arange(len(Errors)) + bar_width *2, time_3, width=bar_width, color=color_time,linewidth=1,edgecolor='black')
# time4Bar = ax.bar(np.arange(len(Errors)) + bar_width * 3, time_4, width=bar_width, color=color_time,linewidth=1,edgecolor='black')
# time5Bar = ax.bar(np.arange(len(Errors)) + bar_width * 4, time_5, width=bar_width, color=color_time,linewidth=1,edgecolor='black')

time1Bar = ax.bar(np.arange(len(Errors)) + bar_width, values, width=bar_width, color=color_time,linewidth=1,edgecolor='black')
time2Bar = ax.bar(np.arange(len(Errors)), tmp, width=bar_width, color=color_time,linewidth=1,edgecolor='black')
time3Bar = ax.bar(np.arange(len(Errors)) + bar_width * 2, tmp, width=bar_width, color=color_time,linewidth=1,edgecolor='black')


# # 平均值横线标注
# plt.axhline(y=value1, xmin=0, xmax=0.3, color='r', linestyle='--', linewidth=1)
# plt.axhline(y=value2, xmin=0.35, xmax=0.65, color='r', linestyle='--', linewidth=1)
# plt.axhline(y=value3, color='r', xmin=0.7, xmax=1, linestyle='--', linewidth=1)

# plt.text(range(len(Errors))[0], value1 + 1, str(value1), color='r', ha='center',fontsize=7,weight='bold')
# plt.text(range(len(Errors))[1], value2 + 1, str(value2), color='r', ha='center',fontsize=7,weight='bold')
# plt.text(range(len(Errors))[2], value3 + 1, str(value3), color='r', ha='center',fontsize=7,weight='bold')

ax.set_ylabel('Avg. Time (s)',fontsize=15,weight='bold')
#ax.set_title('Bar Chart with Two Parts')
# ax.set_yticks(ticks)
ax.set_yticklabels(ax.get_yticklabels(),fontsize=15,rotation=0,weight='bold')
ax.set_xticks(np.arange(len(Errors)) + bar_width)
ax.set_xticklabels(Errors,rotation=0,fontsize=15,weight='bold')
font1 = {'weight' : 'bold','size' : 16}#图例加粗
# ax.legend(prop=font1,loc='upper left')
# ax.legend(loc='upper left', bbox_to_anchor=(0, 1), prop=font1)
# 显示柱状图
# plt.show()
bwith = 2
ax.spines['bottom'].set_linewidth(bwith)
ax.spines['left'].set_linewidth(bwith)
ax.spines['top'].set_linewidth(bwith)
ax.spines['right'].set_linewidth(bwith)


# for i in range(len(Errors)):
#     plt.text(i, nonFailure_time[i] + 0.5, str(nonFailure_time[i]), ha='center',fontsize=4.5,weight='bold')
# for i in range(len(Errors)):
#     plt.text(i + bar_width, failure_time[i] , str(failure_time[i]), ha='center',fontsize=4.5,weight='bold')
# plt.yscale('log')
fig.set_size_inches(7,3)
# plt.gca().spines['top'].set_visible(False)
# plt.gca().spines['right'].set_visible(False)
# plt.show()
plt.tight_layout()
plt.savefig('/home/gaohan/Scalpel-batfish/eval_data_nsdi26/ipran-diff-err.pdf')

print("Done!")
# # 最后关闭图表
# plt.close()