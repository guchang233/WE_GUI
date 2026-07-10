#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
提取 WeCommands.java 中的硬编码中文，替换为翻译键，并生成 i18n JSON。
"""

import json
import re
from pathlib import Path
from collections import OrderedDict, defaultdict

SRC = Path("src/main/java/com/sow/wegui/commands/WeCommands.java")
OUT_SRC = Path("src/main/java/com/sow/wegui/commands/WeCommands.java")
OUT_ZH = Path("src/main/resources/assets/wegui/lang/zh_cn.generated.json")
OUT_EN = Path("src/main/resources/assets/wegui/lang/en_us.generated.json")
OUT_LOG = Path("scripts/i18n_extract_result.json")

PARAM_VAR_PREFIX = {
    "PATTERN": "wegui.param.pattern",
    "MASK": "wegui.param.mask",
    "FROM_MASK": "wegui.param.from_mask",
    "TO_PATTERN": "wegui.param.to_pattern",
    "DIRECTION": "wegui.param.direction",
    "REVOLVE_AXIS": "wegui.param.axis",
    "HV_FLAG": "wegui.param.hv_flag",
    "UP_FG": "wegui.param.up_fg",
    "BIOMEINFO_PT": "wegui.param.biomeinfo_pt",
    "FORMAT": "wegui.param.format",
    "TREE_TYPE": "wegui.param.tree_type",
    "FOREST_TYPE": "wegui.param.forest_type",
    "AMOUNT": "wegui.param.amount",
    "RADIUS": "wegui.param.radius",
    "RANGE": "wegui.param.range",
    "SIZE": "wegui.param.size",
    "COUNT": "wegui.param.count",
    "HEIGHT": "wegui.param.height",
    "ITERATIONS": "wegui.param.iterations",
    "DISTANCE": "wegui.param.distance",
    "DEPTH": "wegui.param.depth",
    "THICKNESS": "wegui.param.thickness",
    "EXPRESSION": "wegui.param.expression",
    "FILENAME": "wegui.param.filename",
    "PLAYER": "wegui.param.player",
    "SNAPSHOT_NAME": "wegui.param.snapshot_name",
    "DATE": "wegui.param.date",
    "SCRIPT": "wegui.param.script",
    "QUERY": "wegui.param.query",
    "STEPS": "wegui.param.steps",
    "OFFSET": "wegui.param.offset",
    "BUTCHER_FLAGS": "wegui.param.butcher_flags",
    "DENSITY": "wegui.param.density",
}

OPTION_LIST_PREFIX = {
    "DIRECTION_OPTIONS": "wegui.option.direction",
    "AXIS_XYZ_OPTIONS": "wegui.option.axis",
    "HV_OPTIONS": "wegui.option.hv",
    "UP_FG_OPTIONS": "wegui.option.up_fg",
    "BIOMEINFO_PT_OPTIONS": "wegui.option.biomeinfo_pt",
    "FORMAT_OPTIONS": "wegui.option.format",
    "TREE_TYPE_OPTIONS": "wegui.option.tree",
}


def slugify_var(name: str) -> str:
    return name.lower().replace("_", ".")


def contains_chinese(s: str) -> bool:
    return any("\u4e00" <= c <= "\u9fff" for c in s)


class Extractor:
    def __init__(self):
        self.translations = OrderedDict()
        self.current_command_id = None
        self.current_param_var = None
        self.current_option_list = None
        self.inline_param_index = 0
        self.inline_option_index = 0

    def add(self, key: str, zh: str):
        if key not in self.translations:
            self.translations[key] = {"zh": zh, "en": ""}

    def register_key(self, id: str, kind: str, zh: str) -> str:
        key = f"wegui.command.{id}.{kind}"
        self.add(key, zh)
        return key

    def usage_key(self, id: str, usage_id: str, zh: str) -> str:
        key = f"wegui.command.{id}.usage.{usage_id}.description"
        self.add(key, zh)
        return key

    def usage_template_key(self, id: str, usage_id: str, zh: str) -> str:
        key = f"wegui.command.{id}.usage.{usage_id}.template"
        self.add(key, zh)
        return key

    def shared_param_name_key(self, var: str, zh: str) -> str:
        prefix = PARAM_VAR_PREFIX.get(var, f"wegui.param.{slugify_var(var)}")
        key = f"{prefix}.name"
        self.add(key, zh)
        return key

    def shared_param_desc_key(self, var: str, zh: str) -> str:
        prefix = PARAM_VAR_PREFIX.get(var, f"wegui.param.{slugify_var(var)}")
        key = f"{prefix}.description"
        self.add(key, zh)
        return key

    def inline_param_name_key(self, id: str, zh: str) -> str:
        key = f"wegui.command.{id}.param.{self.inline_param_index}.name"
        self.add(key, zh)
        return key

    def inline_param_desc_key(self, id: str, zh: str) -> str:
        key = f"wegui.command.{id}.param.{self.inline_param_index}.description"
        self.add(key, zh)
        return key

    def shared_option_label_key(self, var: str, value: str, zh: str) -> str:
        prefix = OPTION_LIST_PREFIX.get(var, f"wegui.option.{slugify_var(var)}")
        key = f"{prefix}.{value}.label"
        self.add(key, zh)
        return key

    def shared_option_tooltip_key(self, var: str, value: str, zh: str) -> str:
        prefix = OPTION_LIST_PREFIX.get(var, f"wegui.option.{slugify_var(var)}")
        key = f"{prefix}.{value}.tooltip"
        self.add(key, zh)
        return key

    def inline_option_label_key(self, id: str, zh: str) -> str:
        key = f"wegui.command.{id}.option.{self.inline_option_index}.label"
        self.add(key, zh)
        return key

    def inline_option_tooltip_key(self, id: str, zh: str) -> str:
        key = f"wegui.command.{id}.option.{self.inline_option_index}.tooltip"
        self.add(key, zh)
        return key

    def process(self):
        src_text = SRC.read_text(encoding="utf-8")
        lines = src_text.splitlines(keepends=True)
        new_lines = []

        for raw_line in lines:
            line = raw_line

            # 检测共享 Param 常量声明（含 description 的 4 参数形式）
            m = re.search(r'private static final Param (\w+) = new Param\("([^"]+)", (ParamType\.\w+), "([^"]*)", "([^"]+)"\);', line)
            if m:
                self.current_param_var = m.group(1)
                self.current_command_id = None
                self.current_option_list = None
                self.inline_param_index = 0
                self.inline_option_index = 0
                var = m.group(1)
                name_zh = m.group(2)
                desc_zh = m.group(5)
                name_key = self.shared_param_name_key(var, name_zh)
                desc_key = self.shared_param_desc_key(var, desc_zh)
                line = line.replace(f'"{name_zh}"', f'"{name_key}"', 1)
                line = line.replace(f'"{desc_zh}"', f'"{desc_key}"', 1)

            # 检测其他共享 Param 常量声明（无 description 或 5+ 参数）
            m = re.search(r'private static final Param (\w+) = new Param\("([^"]+)", (ParamType\.\w+)', line)
            if m and not re.search(r'private static final Param \w+ = new Param\("[^"]+", ParamType\.\w+, "[^"]*", "[^"]+"\);', line):
                self.current_param_var = m.group(1)
                self.current_command_id = None
                self.current_option_list = None
                self.inline_param_index = 0
                self.inline_option_index = 0
                var = m.group(1)
                name_zh = m.group(2)
                name_key = self.shared_param_name_key(var, name_zh)
                line = line.replace(f'"{name_zh}"', f'"{name_key}"', 1)
                # 同一行内可能还有中文 description
                for desc_zh in re.findall(r'"([^"]+)"', line):
                    if contains_chinese(desc_zh):
                        desc_key = self.shared_param_desc_key(var, desc_zh)
                        line = line.replace(f'"{desc_zh}"', f'"{desc_key}"', 1)
                        break

            # 检测共享 Option 列表声明
            m = re.search(r'private static final List<Option> (\w+)\s*=\s*List\.of\(', line)
            if m:
                self.current_option_list = m.group(1)
                self.current_param_var = None
                self.current_command_id = None
                self.inline_param_index = 0
                self.inline_option_index = 0

            # 检测 register 调用
            m = re.search(r'register\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)"', line)
            if m:
                self.current_command_id = m.group(1)
                self.current_param_var = None
                self.current_option_list = None
                self.inline_param_index = 0
                self.inline_option_index = 0
                cmd_id = m.group(1)
                name_zh = m.group(2)
                desc_zh = m.group(3)
                name_key = self.register_key(cmd_id, "name", name_zh)
                desc_key = self.register_key(cmd_id, "description", desc_zh)
                line = line.replace(f'register("{cmd_id}", "{name_zh}", "{desc_zh}"',
                                    f'register("{cmd_id}", "{name_key}", "{desc_key}"', 1)

            # 替换 instant 描述
            def repl_instant(m):
                usage_id = m.group(1)
                cmd = m.group(2)
                desc_zh = m.group(3)
                prefix = self.current_command_id or "wegui.command"
                key = self.usage_key(prefix, usage_id, desc_zh)
                return f'instant("{usage_id}", "{cmd}", "{key}"'
            line = re.sub(r'instant\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)"', repl_instant, line)

            # 替换 param 描述与 template（如含中文）
            def repl_param(m):
                usage_id = m.group(1)
                base = m.group(2)
                template = m.group(3)
                desc_zh = m.group(4)
                prefix = self.current_command_id or "wegui.command"
                desc_key = self.usage_key(prefix, usage_id, desc_zh)
                if contains_chinese(template):
                    template_key = self.usage_template_key(prefix, usage_id, template)
                else:
                    template_key = template
                return f'param("{usage_id}", "{base}", "{template_key}", "{desc_key}"'
            line = re.sub(r'param\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)"', repl_param, line)

            # 替换 bind 描述与 template（如含中文）
            def repl_bind(m):
                usage_id = m.group(1)
                base = m.group(2)
                template = m.group(3)
                desc_zh = m.group(4)
                prefix = self.current_command_id or "wegui.command"
                desc_key = self.usage_key(prefix, usage_id, desc_zh)
                if contains_chinese(template):
                    template_key = self.usage_template_key(prefix, usage_id, template)
                else:
                    template_key = template
                return f'bind("{usage_id}", "{base}", "{template_key}", "{desc_key}"'
            line = re.sub(r'bind\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)"', repl_bind, line)

            # 替换 bindNoParam 描述
            def repl_bind_no_param(m):
                usage_id = m.group(1)
                cmd = m.group(2)
                desc_zh = m.group(3)
                prefix = self.current_command_id or "wegui.command"
                key = self.usage_key(prefix, usage_id, desc_zh)
                return f'bindNoParam("{usage_id}", "{cmd}", "{key}"'
            line = re.sub(r'bindNoParam\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)"', repl_bind_no_param, line)

            # 替换 biomeParam / entityTypeParam 中的中文 name
            def repl_biome_param(m):
                name_zh = m.group(1)
                default_value = m.group(2)
                optional = m.group(3)
                if not self.current_command_id:
                    return m.group(0)
                key = self.inline_param_name_key(self.current_command_id, name_zh)
                self.inline_param_index += 1
                return f'biomeParam("{key}", "{default_value}", {optional})'
            line = re.sub(r'biomeParam\("([^"]+)",\s*"([^"]+)",\s*(true|false)\)', repl_biome_param, line)

            def repl_entity_type_param(m):
                name_zh = m.group(1)
                default_value = m.group(2)
                optional = m.group(3)
                if not self.current_command_id:
                    return m.group(0)
                key = self.inline_param_name_key(self.current_command_id, name_zh)
                self.inline_param_index += 1
                return f'entityTypeParam("{key}", "{default_value}", {optional})'
            line = re.sub(r'entityTypeParam\("([^"]+)",\s*"([^"]+)",\s*(true|false)\)', repl_entity_type_param, line)

            # 处理 new Option("value", "label", "tooltip")，value 可为空
            def repl_option(m):
                value = m.group(1)
                label_zh = m.group(2)
                tooltip_zh = m.group(3)
                if self.current_option_list:
                    label_key = self.shared_option_label_key(self.current_option_list, value, label_zh)
                    tooltip_key = self.shared_option_tooltip_key(self.current_option_list, value, tooltip_zh)
                elif self.current_command_id:
                    label_key = self.inline_option_label_key(self.current_command_id, label_zh)
                    tooltip_key = self.inline_option_tooltip_key(self.current_command_id, tooltip_zh)
                    self.inline_option_index += 1
                else:
                    return m.group(0)
                return f'new Option("{value}", "{label_key}", "{tooltip_key}")'
            line = re.sub(r'new Option\("([^"]*)",\s*"([^"]+)",\s*"([^"]+)"\)', repl_option, line)

            # 处理 inline new Param("中文名", ParamType.XXX, ..., "中文desc")
            # 匹配到行内完整闭合的 new Param(...)
            def repl_param_inline(m):
                full = m.group(0)
                name_zh = m.group(1)
                body = m.group(3)
                # 找 body 中最后一个含中文的字符串作为 description
                strings = re.findall(r'"([^"]*)"', body)
                desc_zh = None
                for s in reversed(strings):
                    if contains_chinese(s):
                        desc_zh = s
                        break
                if self.current_command_id:
                    name_key = self.inline_param_name_key(self.current_command_id, name_zh)
                    if desc_zh:
                        desc_key = self.inline_param_desc_key(self.current_command_id, desc_zh)
                    self.inline_param_index += 1
                else:
                    return full
                full = full.replace(f'"{name_zh}"', f'"{name_key}"', 1)
                if desc_zh:
                    full = full.replace(f'"{desc_zh}"', f'"{desc_key}"', 1)
                return full
            line = re.sub(r'new Param\("([^"]+)",\s*(ParamType\.\w+)(.*?)\)', repl_param_inline, line)

            new_lines.append(line)

        OUT_SRC.write_text("".join(new_lines), encoding="utf-8")

        zh_entries = OrderedDict((k, v["zh"]) for k, v in self.translations.items())
        en_entries = OrderedDict((k, v["en"]) for k, v in self.translations.items())
        OUT_ZH.write_text(json.dumps(zh_entries, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        OUT_EN.write_text(json.dumps(en_entries, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        OUT_LOG.write_text(json.dumps(self.translations, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

        print(f"Extracted {len(self.translations)} translation keys.")


if __name__ == "__main__":
    Extractor().process()
