#先运行select_node.py
#input：1. 配置文件路径 2. 生成的文件路径
#output：随机选择的要加入策略的节点（文件路径/邻居ip/邻居name） 【写入文件】


#再运行add-polic.py
#input：select_node.py生成的文件路径
#output：修改后的配置文件（直接在原始位置修改，注意保存备份修改前的配置）