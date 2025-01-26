#include "EmergencyEvent.h"
#include <sstream>
#include <iomanip>
#include <stdexcept>
#include <algorithm>
#include <algorithm>
#include <iostream>
#include <string>

using namespace std;



// מפת הסיכומים לפי ערוצים
map<string, map<string, vector<EmergencyEvent>>> eventSummaryMap;
map<string, shared_ptr<mutex>> channelMutexes;

EmergencyEvent::EmergencyEvent(const Event& e) 
    : Event(e), formatDateTime(""), active(false), forcesArrival(false){
    this->formatDateTime = epochToTimeAndDate(e.get_date_time());
    this->active = isFieldTrue("active", e.get_general_information());
    this->forcesArrival = isFieldTrue("forces_arrival_at_scene", e.get_general_information());
}



// השוואה בין אירועים לצורך מיון
bool EmergencyEvent::operator<(const EmergencyEvent& other) const {
    if (this->get_date_time() != other.get_date_time()) {
        return this->get_date_time() < other.get_date_time();
    }
    return this->get_name() < other.get_name();
}

// פונקציה להוספת אירוע לערוץ בסיכום
void addToSummary(const Event& e, const string& username) {
    // וודא שלערוץ יש מנעול
    ensureChannelMutexExists(e.get_channel_name());
    // נעילת המנעול של הערוץ המסוים
    lock_guard<mutex> lock(*channelMutexes[e.get_channel_name()]);
    // יצירת האירוע
    EmergencyEvent eventSummary(e);
    // הוספת האירוע לערוץ ולמשתמש המתאים
    eventSummaryMap[e.get_channel_name()][username].push_back(eventSummary);
    // מיון האירועים של המשתמש בתוך הערוץ
    sort(eventSummaryMap[e.get_channel_name()][username].begin(),
         eventSummaryMap[e.get_channel_name()][username].end());
}


void ensureChannelMutexExists(const string& channelName) {
    static mutex mutexForMutexes; // מנעול להגנה על map המנעולים
    lock_guard<mutex> lock(mutexForMutexes);

    if (channelMutexes.find(channelName) == channelMutexes.end()) {
        channelMutexes.emplace(channelName, make_shared<mutex>());
    }
}
string epochToTimeAndDate(int epochTime) {
    // המרת ה-epoch ל-time_t
    time_t rawTime = static_cast<std::time_t>(epochTime);

    // המרה למבנה זמן קריא
    tm* timeInfo = std::localtime(&rawTime);

    // המרת הזמן למחרוזת בפורמט הרצוי
    std::ostringstream oss;
    oss << std::put_time(timeInfo, "%d/%m/%Y_%H:%M");
    return oss.str();
}

// פונקציה להמרת תאריך לפורמט הנדרש
string formatToDateTime(const string& rawDateTime) {
    istringstream input(rawDateTime);
    ostringstream output;
    int year, month, day, hour, minute, second;
    char dash1, dash2, space, colon1, colon2;

    input >> year >> dash1 >> month >> dash2 >> day >> space >> hour >> colon1 >> minute >> colon2 >> second;
    if (input.fail()) {
        cerr << "Invalid date format" << endl;
        return "-1";
    }

    output << setfill('0') << setw(2) << day << "/"
           << setw(2) << month << "/"
           << year << " "
           << setw(2) << hour << ":"
           << setw(2) << minute;

    return output.str();
}


bool EmergencyEvent::isFieldTrue(const string& fieldName, const map<string, string>& generalInfo) const {
    auto it = generalInfo.find(fieldName);
    if (it != generalInfo.end()) {
        const std::string& value = it->second;

        // בדיקה אם הערך הוא "true" (מחרוזת)
        if (value == "true") {
            return true;
        }

        // נסיון לפרש את הערך כבוליאני
        if (value == "1") { // ייתכן ששדה מגיע כ-"1" המייצג true
            return true;
        }
    }

    return false;
}



// פונקציות גישה
const string& EmergencyEvent::getFormatedDateTime() const {
    return this->formatDateTime; 
}

const bool EmergencyEvent:: getActive() const {return this->active;}
const bool EmergencyEvent:: getForcesArrival() const {return this->forcesArrival;}

