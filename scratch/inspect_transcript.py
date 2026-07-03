import json

transcript_path = r"C:\Users\sande\.gemini\antigravity\brain\2c60f77e-685e-4efa-a118-89469c4b6943\.system_generated\logs\transcript.jsonl"

with open(transcript_path, "r", encoding="utf-8") as f:
    for i in range(15):
        line = f.readline()
        if not line:
            break
        data = json.loads(line)
        print(f"Index: {data.get('step_index')}, Source: {data.get('source')}, Type: {data.get('type')}, Status: {data.get('status')}")
        print("Keys:", list(data.keys()))
        if "tool_calls" in data:
            print("Tool calls count:", len(data["tool_calls"]))
            for tc in data["tool_calls"]:
                print("  Tool name:", tc.get("name"))
        print("-" * 40)
