#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
使用 googletrans 把 en_us.json 中值为空的条目从中文翻译成英文。
"""

import json
import time
from pathlib import Path
from googletrans import Translator

EN_PATH = Path("src/main/resources/assets/wegui/lang/en_us.json")
ZH_PATH = Path("src/main/resources/assets/wegui/lang/zh_cn.json")
LOG_PATH = Path("scripts/i18n_extract_result.json")


def main():
    translator = Translator()
    en_data = json.loads(EN_PATH.read_text(encoding="utf-8"))
    zh_data = json.loads(ZH_PATH.read_text(encoding="utf-8"))
    log_data = json.loads(LOG_PATH.read_text(encoding="utf-8"))

    empty_keys = [k for k, v in en_data.items() if v == ""]
    print(f"Translating {len(empty_keys)} entries...")

    for idx, key in enumerate(empty_keys):
        zh_text = zh_data.get(key) or log_data.get(key, {}).get("zh", "")
        if not zh_text:
            print(f"  [{idx+1}/{len(empty_keys)}] No source for {key}")
            continue
        try:
            result = translator.translate(zh_text, src="zh-cn", dest="en")
            en_data[key] = result.text
            print(f"  [{idx+1}/{len(empty_keys)}] {key}: {zh_text} -> {result.text}")
        except Exception as e:
            print(f"  [{idx+1}/{len(empty_keys)}] FAILED {key}: {e}")
        time.sleep(0.3)

    EN_PATH.write_text(json.dumps(en_data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Done.")


if __name__ == "__main__":
    main()
