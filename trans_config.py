import re
import os

def extract_interface_blocks(config_text):
    pattern = r'(interface \S+)(.*?)(?=^interface|\Z)'  # 匹配每个接口配置块
    return re.findall(pattern, config_text, flags=re.S | re.M)

def get_ospf_interfaces(config_text):
    interface_blocks = extract_interface_blocks(config_text)
    ospf_interfaces = []

    for header, body in interface_blocks:
        if "ip ospf" in body:
            ip_match = re.search(r'ip address (\d+\.\d+\.\d+\.\d+) (\d+\.\d+\.\d+\.\d+)', body)
            if ip_match:
                ip, mask = ip_match.groups()
                ospf_interfaces.append((ip, mask))
    return ospf_interfaces

def mask_to_wildcard(mask):
    return '.'.join(str(255 - int(octet)) for octet in mask.split('.'))

def generate_network_commands(ospf_ifaces, area="0"):
    commands = []
    for ip, mask in ospf_ifaces:
        wildcard = mask_to_wildcard(mask)
        commands.append(f" network {ip} {wildcard} area {area}")
    return commands

def remove_ip_ospf_from_interfaces(config_text):
    # 删除每个接口下的 "ip ospf ..." 行
    return re.sub(r'^\s*ip ospf .*\n', '', config_text, flags=re.M)

def patch_config(original_config):
    ospf_ifaces = get_ospf_interfaces(original_config)
    ospf_networks = generate_network_commands(ospf_ifaces)

    # 删除接口下 ospf 命令
    updated_config = remove_ip_ospf_from_interfaces(original_config)

    # 插入 network 命令到 router ospf 区块
    updated_config = re.sub(
        r'(router ospf \d+\n)',
        lambda m: m.group(1) + ''.join(f"{cmd}\n" for cmd in ospf_networks),
        updated_config
    )

    return updated_config

def convert_to_cisco(config_text):
    result = []
    lines = config_text.strip().splitlines()
    current_iface = ""

    for line in lines:
        line = line.rstrip()
        if line.startswith("interface"):
            current_iface = line
            result.append(line)  # 保持原接口名
        elif "encapsulation dot1q" in line.lower():
            # 思科区分大小写，习惯写成 dot1Q
            vlan_id = re.search(r'dot1q\s+(\d+)', line, re.IGNORECASE)
            if vlan_id:
                result.append(f" encapsulation dot1Q {vlan_id.group(1)}")
        else:
            result.append(line)

    return "\n".join(result)

def replace_config(config_text):
    # 替换 LoopBack 为 Loopback
    # config_text = re.sub(r'\binterface LoopBack0\b', 'interface Loopback0', config_text)

    # # 替换 BGP router-id 写法
    # config_text = re.sub(r'bgp router-id (\d+\.\d+\.\d+\.\d+)', r'router bgp 65270\n router-id \1', config_text)
    
    # config_text = re.sub(r'\btoCR peer-group\b', 'neighbor toCR peer-group', config_text)


    return config_text

def remove_lines_starting_with(folder, target_str):
    for root, dirs, files in os.walk(folder):
        for file in files:
            file_path = os.path.join(root, file)
            with open(file_path, "r") as f:
                config_data = f.read()
            converted = convert_to_cisco(config_data)
            patched_config = patch_config(converted)
            final_config = replace_config(patched_config)

            # final_config = replace_config(config_data)
            with open(file_path, "w") as f:
                f.write(final_config)
                print(f"update {file_path}")


if __name__ == "__main__":
    folder = "/home/gaohan/Scalpel-batfish/dataset-new/Ipmetro-1306/configs"
    # target_str = ["route-map", "ip community-list", "as-path", "set","match"]
    target_str = ["redistribute"]
    remove_lines_starting_with(folder, target_str)
