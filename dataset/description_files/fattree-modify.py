import re
import os

def replace_subnet_mask(file_path):
    try:
        with open(file_path, 'r') as file:
            content = file.read()

        # 使用修正后的正则表达式，匹配 IP 地址部分
        pattern = re.compile(r'(network \d+\.\d+\.\d+\.\d+)/\d+')

        # 替换匹配到的字符串中的子网掩码部分
        replaced_content = pattern.sub(r'\1 mask 255.255.255.0', content)

        # 将替换后的内容写回文件
        with open(file_path, 'w') as file:
            file.write(replaced_content)

        print(f"替换成功: {file_path}")

    except Exception as e:
        print(f"替换失败: {file_path}. 错误: {str(e)}")

# 替换指定目录下所有文件中的子网掩码
directory_path = '/home/dell/hg/batfish-condition/dateset/fattree-cfg/fattree32/configs'
for filename in os.listdir(directory_path):
    if filename.endswith('.cfg'):  # 假设配置文件的扩展名是 .cfg
        file_path = os.path.join(directory_path, filename)
        replace_subnet_mask(file_path)
