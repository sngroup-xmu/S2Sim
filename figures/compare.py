import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

# 读取 Excel 文件
df = pd.read_excel('/home/gaohan/Scalpel-batfish/eval_data_nsdi26/comparison.xlsx')

# 提取数据
# topo = df['topo'][:8]#到fattree18
topo = df['topo']
S2sim_time = df['S2Sim']
CPR_time = df['CPR']
CEL_time = df['CEL']
S2sim_time_k = df['S2Sim (K)']
CPR_time_k = df['CPR (K)']
CEL_time_k = df['CEL (K)']
# scalpel_time2 = df['Second Simulation Time']

# 设置柱状图宽度
bar_width = 0.1
gap_between_groups = 0.05

# 设置颜色和填充样式
color_S2sim = '#98D5EE'
color_CPR = '#F5B46F'
color_CEL = '#D46354'
# hatch_pattern1 = '--'  # 斜杠填充
# hatch_pattern2 = "xx"
# hatch_pattern3 = "\\\\"

# 生成两个柱状图
fig, ax = plt.subplots()
S2Sim = ax.bar(np.arange(len(topo)), S2sim_time, width=bar_width, color=color_S2sim, label='S2Sim\'s Diagnosis and Repair Time')
CPR = ax.bar(np.arange(len(topo)) + bar_width, CPR_time, width=bar_width, color=color_CPR, label='CPR\'s Repairing Time')
CEL = ax.bar(np.arange(len(topo)) + bar_width * 2, CEL_time, width=bar_width, color=color_CEL, label='CEL\'s Localization Time')


S2Sim_k = ax.bar(np.arange(len(topo)) + bar_width * 3 + gap_between_groups, S2sim_time_k, width=bar_width, color=color_S2sim)
CPR_k = ax.bar(np.arange(len(topo)) + bar_width * 4 + gap_between_groups, CPR_time_k, width=bar_width, color=color_CPR)
CEL_k = ax.bar(np.arange(len(topo)) + bar_width * 5 + gap_between_groups, CEL_time_k, width=bar_width, color=color_CEL)
# scalpel_time2 = scalpel_time2_top+scalpel_time2_bottom
# time2 = scalpel_time2 +scalpel_time1

# 添加标签和图例
# ax.set_xlabel('Topo',fontsize=15,weight='bold')
ax.set_ylabel('Time (ms)',fontsize=19,weight='bold')
#ax.set_title('Bar Chart with Two Parts')
# ax.set_yticks(ticks)
ax.set_yticklabels(ax.get_yticklabels(),fontsize=15,weight='bold')
ax.set_xticks(np.arange(len(topo)) + bar_width*2)
ax.set_xticklabels(topo,rotation=0,fontsize=15,weight='bold')

font1 = {'weight' : 'bold','size' : 10}#图例加粗
# ax.legend(prop=font1,loc='upper left')
ax.legend(loc='center', bbox_to_anchor=(0.55, 0.85), prop=font1)
# 显示柱状图
# plt.show()
bwith = 2
ax.spines['bottom'].set_linewidth(bwith)
ax.spines['left'].set_linewidth(bwith)
ax.spines['top'].set_linewidth(bwith)
ax.spines['right'].set_linewidth(bwith)


for i in range(len(topo)):
    plt.text(i, S2sim_time[i], str(S2sim_time[i]), ha='center',fontsize=7,weight='bold')
for i in range(len(topo)):
    plt.text(i + bar_width, CPR_time[i] , str(CPR_time[i]), ha='center',fontsize=7,weight='bold')
for i in range(len(topo)):
    plt.text(i + bar_width * 2, CEL_time[i] , str(CEL_time[i]), ha='center',fontsize=7,weight='bold')

for i in range(len(topo)):
    plt.text(i + bar_width * 3 + gap_between_groups, S2sim_time_k[i] + 0.5, str(S2sim_time_k[i]), ha='center',fontsize=7,weight='bold')
for i in range(len(topo)):
    plt.text(i + bar_width * 4 + gap_between_groups, CPR_time_k[i] , str(CPR_time_k[i]), ha='center',fontsize=7,weight='bold')
for i in range(len(topo)):
    # 检查 CEL_time_k[i] 是否为 0，如果是则显示 '>24h'
    text = f'{CEL_time_k[i]}' if CEL_time_k[i] != 0 else '>24h'
    x_positions = i + bar_width * 5 + gap_between_groups if CEL_time_k[i] != 0 else i + bar_width * 5 + gap_between_groups * 2
    y_position = CEL_time_k[i] if CEL_time_k[i] != 0 else 300
    size = 7 if CEL_time_k[i] != 0 else 12
    this_color = "black" if CEL_time_k[i] != 0 else "red"
    # 在图中添加文本
    plt.text(x_positions, y_position, text, ha='center', fontsize = size, weight='bold',color = this_color)

plt.yscale('log')
fig.set_size_inches(9,4)
# plt.gca().spines['top'].set_visible(False)
# plt.gca().spines['right'].set_visible(False)
# plt.show()
plt.savefig('/home/gaohan/Scalpel-batfish/eval_data_nsdi26/evl-comparison.pdf')

print("Done!")
# # 最后关闭图表
# plt.close()