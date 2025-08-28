# Re-import required libraries due to kernel reset
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import matplotlib.ticker as ticker

# Reload the Excel file
df = pd.read_excel('/home/gaohan/Scalpel-batfish/eval_data_nsdi26/Eval-Time-update.xlsx')

# 设置参数：柱子宽度与间距
bar_width = 0.03
gap_within_group = 0
gap_between_groups = 0.03  # 缩短Topology之间的距离

# 获取唯一的Topology Name
topologies = df['Topology Name'].unique()

# 构建横轴与数据
x_positions = []
data = []
gen_vals, diagnosis_repair_vals, total_vals = [], [], []
req_labels = []
topo_labels = []
topo_centers = []

cur_x = 0
for topo in topologies:
    group = df[df['Topology Name'] == topo]
    start_x = cur_x
    for _, row in group.iterrows():
        x_pos = cur_x
        x_positions.append(x_pos)
        total_val = row['Total Time (ms)']
        total_vals.append(total_val)

        if row['Requirement Type'] == 'Reachability':
            req_label = 'K=0'
            req_labels.append(req_label)
        elif row['Requirement Type'] == 'K-failure':
            req_label = 'K=1'
            req_labels.append(req_label)
        
        data.append((x_pos, req_label, total_val))

        cur_x += bar_width + gap_within_group
    center_x = (start_x + cur_x - gap_within_group - bar_width) / 2
    topo_centers.append(center_x)
    topo_labels.append(topo)
    cur_x += gap_between_groups  # 缩短组间距

# 解压绑定的数据
x_positions, req_labels, total_vals = zip(*data)
# 转换为数组
x = np.array(x_positions)
gen = np.array(gen_vals)
diagnosis_repair = np.array(diagnosis_repair_vals)
total = np.array(total_vals)

# 创建图像
fig, ax = plt.subplots(figsize=(8, 6))

# 设置颜色和填充样式
color_1 = '#FF4500'  # 橙色
color_2 = '#3CB371'  # 绿色
color_3 = '#0080FF'  # 蓝色
color_4 = '#4169E1'  # 深蓝色
hatch_pattern1 = '--'
hatch_pattern2 = "xx"
hatch_pattern3 = "//"

# 堆叠柱状图
for (x_pos, req_label, total_val) in data:
    if req_label == 'K=0':
        ax.bar(x_pos, total_val, width=bar_width, color='none', edgecolor=color_1, hatch=hatch_pattern2)
    elif req_label == 'K=1':
        ax.bar(x_pos, total_val, width=bar_width, color='none', edgecolor=color_3, hatch=hatch_pattern3)


# 对数y轴
ax.set_yscale('log')
# ax.set_ylim(1e2, None)
# ax.set_ylim(1e5, None)
ax.set_ylabel('Time (ms)')

# 设置Requirement Type标签，右下倾斜45度
ax.set_xticks(x)
ax.set_xticklabels(req_labels, rotation=-45, ha='left', va='top', rotation_mode='anchor', fontsize=10)
ax.tick_params(axis='x', pad=10)

# 绘制下层Topology标签
for xpos, tlabel in zip(topo_centers, topo_labels):
    ax.text(xpos, -0.13, tlabel, ha='center', va='top', fontsize=12, transform=ax.get_xaxis_transform())

# 调整布局
plt.tight_layout(rect=[0, 0.01, 1, 1])

# 保存图像
pdf_path = "/home/gaohan/Scalpel-batfish/eval_data_nsdi26/test.png"
plt.savefig(pdf_path)
plt.show()