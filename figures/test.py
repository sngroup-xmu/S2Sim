import matplotlib.pyplot as plt
import numpy as np

# 示例数据
topologies = ['Topology1', 'Topology2', 'Topology3']
k0_times = [24, 30, 28]  # K=0的时间
k1_times = [15, 18, 12]  # K=1的时间

# 每个拓扑的数据有两个柱子：一个对应K=0，一个对应K=1
# 因此，每个拓扑的柱子总数为6（3个K=0和3个K=1）

bar_width = 0.35
index = np.arange(len(topologies))

# 创建图形
fig, ax = plt.subplots(figsize=(8, 6))

# 绘制K=0的柱状图
ax.bar(index - bar_width/2, k0_times, bar_width, label='K=0', color='b')

# 绘制K=1的柱状图
ax.bar(index + bar_width/2, k1_times, bar_width, label='K=1', color='g')

# 设置X轴标签
ax.set_ylabel('Time (ms)')
ax.set_title('Comparison of Repair Times by Topology and K')

# 设置X轴刻度和标签
# 每个拓扑名称占一行，K=0和K=1标注为第二行
labels = [f'{topo}\nK=0' for topo in topologies] + [f'{topo}\nK=1' for topo in topologies]
ax.set_xticks(np.arange(len(topologies) * 2))
ax.set_xticklabels(labels)

# 添加图例
ax.legend()

plt.savefig('/home/gaohan/Scalpel-batfish/eval_data_nsdi26/test.png')
