import os
import random
import sys
import re

def random_files_in_directory(directory_path):
    # 获取指定目录中的所有文件
    all_files = [os.path.join(directory_path, file) for file in os.listdir(directory_path) if os.path.isfile(os.path.join(directory_path, file))]

    # 计算1/3的文件数量
    num_files_to_select = len(all_files) // 3

    # 从所有文件中随机选择1/3的文件
    selected_files = random.sample(all_files, num_files_to_select)

    return selected_files

def write_selected_files_to_file(output_file_path, selected_files):
    with open(output_file_path, 'w') as file:
        for file_path in selected_files:
            file.write(file_path + '\n')

def extract_neighbor_info_from_bgp_config(bgp_config):
    # 使用正则表达式找到所有匹配的 neighbor 语句
    neighbor_matches = re.findall(r'neighbor\s+(\S+)\s+remote-as\s+\d+', bgp_config)
    # 找到所有匹配的描述信息
    description_matches = re.findall(r'neighbor\s+\S+\s+description\s+"([^"]+)"', bgp_config)

    # 如果找到了至少一个 neighbor
    if neighbor_matches:
        # 随机选择一个 neighbor 的索引
        random_index = random.randrange(len(neighbor_matches))
        
        # 获取随机选择的 neighbor 的 IP
        neighbor_ip = neighbor_matches[random_index]
        
        # 获取相应索引的描述信息，如果有的话
        neighbor_description = description_matches[random_index] if random_index < len(description_matches) else None    
        return neighbor_ip, neighbor_description.split(" ")[1]
    else:
        print("No neighbor statements found in the BGP configuration.")
    
    return None, None

def process_selected_files(selected_files, output_file):
    for file_path in selected_files:
        # 获取文件名
        file_name = file_path

        # 读取文件内容
        with open(file_path, 'r') as file:
            file_content = file.read()

            # 使用正则表达式从 BGP 配置中提取邻居信息,匹配以 "router bgp " 开头，后面跟着数字（BGP AS号），然后匹配任意字符（包括换行符），直到遇到感叹号 "!" 为止的部分
            bgp_config_match = re.search(r'router bgp \d+(.*?)!', file_content, re.DOTALL)
            if bgp_config_match:
                bgp_config = bgp_config_match.group(1)
                neighbor_ip, neighbor_name = extract_neighbor_info_from_bgp_config(bgp_config)

                # 输出邻居信息
                select_info = "File:" + file_name + ", Neighbor IP:" + neighbor_ip + ", Neighbor Name:" + neighbor_name + "\n"
                print(select_info)

                with open(output_file, 'a') as output:
                    output.write(select_info)

            else:
                print(f"File: {file_name}, No BGP configuration found")

if __name__ == "__main__":
    # 从命令行参数获取输入目录路径和输出文件路径
    if len(sys.argv) != 3:
        print("Usage: python script.py configs_directory output_file")
        sys.exit(1)

    input_directory = sys.argv[1]
    output_file_path = sys.argv[2]

    # 获取随机选择的文件路径
    selected_files = random_files_in_directory(input_directory)

    # 为每个文件提取邻居信息并输出
    process_selected_files(selected_files,output_file_path)
