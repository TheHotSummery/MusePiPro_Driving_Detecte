#pragma once
#include "plc_types.h"
#include <string>
#include <vector>
#include "nlohmann/json.hpp"

// 使用 nlohmann::json
using json = nlohmann::json;

class JSONParser {
public:
    JSONParser() = default;
    ~JSONParser() = default;
    
    bool parse_system_config(const std::string& file, 
                            std::vector<RungConfig>& rungs);

    bool parse_user_config(const std::string& file,
                          std::vector<RungConfig>& rungs,
                          std::vector<TimerConfig>& timers,
                          std::vector<CounterConfig>& counters);
    
    std::string get_last_error() const;
    
private:
    void set_error(const std::string& error);
    std::string last_error_;
};
