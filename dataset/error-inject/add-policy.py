import re
import os

def add_bgp_route_policy(file_path, neighbor_ip, route_map_name):
    # 读取原始配置文件内容
    with open(file_path, 'r') as file:
        config_text = file.read()

    # 定义正则表达式模式，匹配 BGP 配置中的 neighbor 和 route-map
    Bgp_pattern = re.compile(r'router\s+bgp\s+\d')
    neighbor_pattern = re.compile(r'neighbor\s+{}\s+remote-as\s+\d+'.format(re.escape(neighbor_ip)))
    route_map_pattern = re.compile(r'route-map\s+{}\s+permit\s+\d+'.format(re.escape(route_map_name)))

    # 在配置中查找 neighbor 配置
    match_neighbor = neighbor_pattern.search(config_text)

    if match_neighbor:
        # 在 neighbor 配置之后添加 route-map
        config_text = config_text[:match_neighbor.end()] + '\n neighbor ' + neighbor_ip + ' route-map {} in'.format(route_map_name) + config_text[match_neighbor.end():]
    else:
        # 如果找不到 neighbor 配置，抛出异常或采取其他处理方式
        raise ValueError('Neighbor configuration not found for IP {}'.format(neighbor_ip))

    # 在配置中查找 route-map 配置
    match_route_map = route_map_pattern.search(config_text)

    if not match_route_map:
        # 如果找不到 route-map 配置，在BGP配置之前添加一个新的 route-map
        match_bgp = Bgp_pattern.search(config_text)
        #添加的route-map的命令（需要修改内容）
        config_text = config_text[:match_bgp.start()] + 'route-map {} permit 10\n'.format(route_map_name) + ' match community 2\n set local-preference 1\n set community 100:1 additive\n' + 'route-map {} permit 100\n'.format(route_map_name) + '!\n!\n' + config_text[match_bgp.start():]
    return config_text

if __name__ == "__main__":

    # 原文件路径
    configs_path = "/home/dell/hg/synet_py3/mydata/error-injuct/Arnes_abs_simple_2.txt"
    with open(configs_path, 'r') as file:
        for line in file:
            command = line.strip()
            # 定义正则表达式模式
            pattern = re.compile(r'File:(?P<file_path>[\w./-]+), Neighbor IP:(?P<neighbor_ip>\d+\.\d+\.\d+\.\d+), Neighbor Name:(?P<neighbor_name>[\w\s]+)')

            # 使用正则表达式匹配文本
            match = pattern.match(command)

            # 如果匹配成功
            if match:
                # 提取匹配的组
                file_path = match.group('file_path')
                neighbor_ip = match.group('neighbor_ip')
                neighbor_name = match.group('neighbor_name')
                file_name = os.path.basename(file_path).split(".")[0]
                #生成添加rp后的配置
                new_config = add_bgp_route_policy(file_path, neighbor_ip, 'RMap_'+file_name +'_from_' + neighbor_name)

                #将修改后的配置写回文件
                with open(file_path, 'w') as output_file:
                    output_file.write(new_config)
