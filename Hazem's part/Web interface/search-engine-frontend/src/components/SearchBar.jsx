import React, { useState, useEffect, useRef } from "react";
import { getSuggestions } from "../suggestionService";

function SearchBar({ onSearch }) {
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const searchInputRef = useRef(null);

  const inputRef = useRef(null);
  const [inputWidth, setInputWidth] = useState("auto");

  useEffect(() => {
    if (inputRef.current) {
      setInputWidth(`${inputRef.current.offsetWidth}px`);
    }
  }, [query]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (
        searchInputRef.current &&
        !searchInputRef.current.contains(event.target)
      ) {
        setShowSuggestions(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

const handleInputChange = async (e) => {
  const value = e.target.value;
  setQuery(value);

  if (value.length > 0) {
    const suggs = await getSuggestions(value);
    setSuggestions(suggs);
    setShowSuggestions(true);
  } else {
    setSuggestions([]);
    setShowSuggestions(false);
  }
};

  const handleSubmit = (e) => {
    e.preventDefault();
    if (query.trim()) {
      onSearch(query);
      setShowSuggestions(false);
    }
  };

  const handleSuggestionClick = (suggestion) => {
    setQuery(suggestion);
    onSearch(suggestion);
    setShowSuggestions(false);
  };

  return (
    <form className="search-bar" onSubmit={handleSubmit}>
      <div className="search-input-container" ref={searchInputRef}>
        <input
          ref={inputRef}
          type="text"
          value={query}
          onChange={handleInputChange}
          placeholder="Search the web..."
          aria-label="Search"
        />
        <button type="submit">Search</button>

        {showSuggestions && suggestions.length > 0 && (
          <ul className="suggestions-list" style={{ width: inputWidth }}>
            {suggestions.map((suggestion, index) => {
              const lowerQuery = query.toLowerCase();
              const lowerSuggestion = suggestion.toLowerCase();
              const matchIndex = lowerSuggestion.indexOf(lowerQuery);

              return (
                <li
                  key={index}
                  onClick={() => handleSuggestionClick(suggestion)}
                >
                  {matchIndex >= 0 ? (
                    <>
                      {suggestion.substring(0, matchIndex)}
                      <span className="match">
                        {suggestion.substring(
                          matchIndex,
                          matchIndex + query.length
                        )}
                      </span>
                      {suggestion.substring(matchIndex + query.length)}
                    </>
                  ) : (
                    suggestion
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </form>
  );
}

export default SearchBar;
