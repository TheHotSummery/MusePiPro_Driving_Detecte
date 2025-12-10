#include "json_parser.h"
#include <fstream>
#include <iostream>

// Helper to parse a single condition
static Condition parse_condition_obj(const json& j) {
    std::string type = j.value("type", "");
    bool normally_open = j.value("normally_open", true);
    
    if (type == "input") {
        return Condition(ConditionType::INPUT, j.value("input", ""), normally_open);
    } else if (type == "output") {
        return Condition(ConditionType::OUTPUT, j.value("output", ""), normally_open);
    } else if (type == "memory") {
        return Condition(ConditionType::MEMORY, j.value("memory", ""), normally_open);
    } else if (type == "timer") {
        return Condition(ConditionType::TIMER, j.value("timer", ""), normally_open);
    } else if (type == "counter") {
        return Condition(ConditionType::COUNTER, j.value("counter", ""), normally_open);
    }
    return Condition(ConditionType::INVALID, "", false);
}

// Helper to parse a single action
static Action parse_action_obj(const json& j) {
    std::string type = j.value("type", "");

    if (type == "output") {
        return Action(ActionType::OUTPUT, j.value("output", ""));
    } else if (type == "set") {
        return Action(ActionType::SET, j.value("output", ""));
    } else if (type == "reset") {
        return Action(ActionType::RESET, j.value("output", ""));
    } else if (type == "memory_set") {
        return Action(ActionType::MEMORY_SET, j.value("memory", ""));
    } else if (type == "memory_reset") {
        return Action(ActionType::MEMORY_RESET, j.value("memory", ""));
    } else if (type == "timer") {
        return Action(ActionType::TIMER, j.value("timer", ""));
    } else if (type == "reset_timer") {
        return Action(ActionType::RESET_TIMER, j.value("timer", ""));
    } else if (type == "counter") {
        return Action(ActionType::COUNTER, j.value("counter", ""));
    } else if (type == "reset_counter") {
        return Action(ActionType::RESET_COUNTER, j.value("counter", ""));
    }
    return Action(ActionType::INVALID, "");
}

// Helper to parse a single rung
static RungConfig parse_rung_obj(const json& j, const std::string& source) {
    RungConfig rung(
        j.value("id", "default_id"),
        j.value("enabled", true),
        source
    );

    if (j.contains("conditions") && j["conditions"].is_array()) {
        for (const auto& cond_json : j["conditions"]) {
            Condition c = parse_condition_obj(cond_json);
            if(c.type != ConditionType::INVALID) {
                rung.conditions.push_back(c);
            }
        }
    }

    if (j.contains("action") && j["action"].is_object()) {
        rung.action = parse_action_obj(j["action"]);
    }
    
    return rung;
}

bool JSONParser::parse_system_config(const std::string& file, 
                                    std::vector<RungConfig>& rungs) {
    std::ifstream ifs(file);
    if (!ifs.is_open()) {
        set_error("Cannot open file: " + file);
        return false;
    }
    
    try {
        json data = json::parse(ifs);
        if (data.contains("rungs") && data["rungs"].is_array()) {
            for (const auto& rung_json : data["rungs"]) {
                rungs.push_back(parse_rung_obj(rung_json, "system"));
            }
        }
    } catch (json::parse_error& e) {
        set_error("JSON parse error in " + file + ": " + e.what());
        return false;
    }

    return true;
}

bool JSONParser::parse_user_config(const std::string& file,
                                  std::vector<RungConfig>& rungs,
                                  std::vector<TimerConfig>& timers,
                                  std::vector<CounterConfig>& counters) {
    std::ifstream ifs(file);
    if (!ifs.is_open()) {
        set_error("Cannot open file: " + file);
        return false;
    }
    
    json j;
    try {
        ifs >> j;
    } catch (json::parse_error& e) {
        set_error("JSON parse error in " + file + ": " + e.what());
        return false;
    }
    
    // Parse timers
    if (j.contains("timers")) {
        if (j["timers"].is_array()) {
            for (const auto& timer_json : j["timers"]) {
                timers.emplace_back(
                    timer_json.value("name", ""),
                    timer_json.value("preset", 0.0),
                    timer_json.value("alias", "")
                );
            }
        }
    }

    // Parse counters
    if (j.contains("counters")) {
        if (j["counters"].is_array()) {
            for (const auto& counter_json : j["counters"]) {
                counters.emplace_back(
                    counter_json.value("name", ""),
                    counter_json.value("preset", 0),
                    counter_json.value("alias", "")
                );
            }
        }
    }

    // Parse rungs
    if (j.contains("rungs") && j["rungs"].is_array()) {
        for (const auto& rung_json : j["rungs"]) {
            rungs.push_back(parse_rung_obj(rung_json, "user"));
        }
    }

    return true;
}

std::string JSONParser::get_last_error() const {
    return last_error_;
}

void JSONParser::set_error(const std::string& error) {
    last_error_ = error;
    std::cerr << "JSONParser Error: " << error << std::endl;
}
