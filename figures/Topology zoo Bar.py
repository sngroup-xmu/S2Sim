import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

# 读取 Excel 文件
df = pd.read_excel('/home/gaohan/Scalpel-batfish/eval_data_nsdi26/Topology zoo Time.xlsx')

# 提取数据
# topo = df['topo'][:8]#到fattree18
topo = df['topo']
nonFailure_time = df['Simulation Time']
failure_time = df['Simulation Time(K)']
# scalpel_time2 = df['Second Simulation Time']

# 设置柱状图宽度
bar_width = 0.3

# 设置颜色和填充样式
color_nonFailure = '#c5defa'
color_failure = '#016190'
# color_scalpel2 = '#F0A19A'
# hatch_pattern1 = '--'  # 斜杠填充
# hatch_pattern2 = "xx"
# hatch_pattern3 = "\\\\"

# 生成两个柱状图
fig, ax = plt.subplots()
nonFailureBar = ax.bar(np.arange(len(topo)), nonFailure_time, width=bar_width, color=color_nonFailure, label='k = 0')
failureBar = ax.bar(np.arange(len(topo)) + bar_width, failure_time, width=bar_width, color=color_failure, label='k = 1')
# scalpel_time2_top = ax.bar(np.arange(len(topo)) + bar_width, scalpel_time2, bottom=scalpel_time1, width=bar_width, color=color_scalpel2, hatch=hatch_pattern3, label='Second Simulation Time (R)')

# scalpel_time2 = scalpel_time2_top+scalpel_time2_bottom
# time2 = scalpel_time2 +scalpel_time1

# 添加标签和图例
# ax.set_xlabel('Topo',fontsize=15,weight='bold')

ax.set_ylabel('Time(s)',fontsize=19,weight='bold')
#ax.set_title('Bar Chart with Two Parts')
# ax.set_yticks(ticks)
ax.set_yticklabels(ax.get_yticklabels(),fontsize=15,rotation=0,weight='bold')
ax.set_xticks(np.arange(len(topo)) + bar_width / 2)
ax.set_xticklabels(topo,rotation=20,fontsize=15,weight='bold')
font1 = {'weight' : 'bold','size' : 16}#图例加粗
# ax.legend(prop=font1,loc='upper left')
ax.legend(loc='upper left', bbox_to_anchor=(0, 1), prop=font1)
# 显示柱状图
# plt.show()
bwith = 2
ax.spines['bottom'].set_linewidth(bwith)
ax.spines['left'].set_linewidth(bwith)
ax.spines['top'].set_linewidth(bwith)
ax.spines['right'].set_linewidth(bwith)


for i in range(len(topo)):
    plt.text(i, nonFailure_time[i] + 0.5, str(nonFailure_time[i]), ha='center',fontsize=4.5,weight='bold')
for i in range(len(topo)):
    plt.text(i + bar_width, failure_time[i] , str(failure_time[i]), ha='center',fontsize=4.5,weight='bold')
# plt.yscale('log')
fig.set_size_inches(9,4)
# plt.gca().spines['top'].set_visible(False)
# plt.gca().spines['right'].set_visible(False)
# plt.show()
plt.tight_layout()
plt.savefig('/home/gaohan/Scalpel-batfish/eval_data_nsdi26/TopologyZoo.pdf')

print("Done!")
# # 最后关闭图表
# plt.close()