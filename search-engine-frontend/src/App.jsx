import { useState } from "react";
import SearchBar from "./components/SearchBar";
import SearchResults from "./components/SearchResults";
import Pagination from "./components/Pagination";
import "./styles/App.css";
import { saveQuery } from "./suggestionService";
import axios from "axios";

function App() {
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchTime, setSearchTime] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalResults, setTotalResults] = useState(0);
  const resultsPerPage = 10;

  const handleSearch = async (query) => {
    setLoading(true);
    const startTime = performance.now();

    try {
      // Replace with actual API call to your Java backend
      const response = await searchAPI(query);

      saveQuery(query); // Save the query for suggestions
      
      console.log(response);

      setResults(response.results);
      setTotalResults(response.total);
      setCurrentPage(1);
    } catch (error) {
      console.error("Search failed:", error);
    } finally {
      setLoading(false);
      setSearchTime((performance.now() - startTime) / 1000);
    }
  };

  // Mock API function - replace with actual fetch
  const searchAPI = async (query) => {
    try {
      const response = await axios.get("http://localhost:8080/api/search", {
        params: { query },
      });
      return response.data; // The API response will match the expected structure
    } catch (error) {
      console.error("Error fetching search results:", error);
      throw error;
    }
  };

  // Calculate current results to display
  const indexOfLastResult = currentPage * resultsPerPage;
  const indexOfFirstResult = indexOfLastResult - resultsPerPage;
  const currentResults = results.slice(indexOfFirstResult, indexOfLastResult);

  return (
    <div className="app">
      <header className="app-header">
        <img src="/tomato-logo.png" alt="Tomato Logo" className="logo" />
        <h1>Tomatoes </h1>
        <img src="/tomato-logo.png" alt="Tomato Logo" className="logo" />
      </header>


      <main className="app-main">
        <SearchBar onSearch={handleSearch} />

        {loading && <div className="loading">Searching...</div>}

        {!loading && results.length > 0 && (
          <>
            <div className="search-info">
              Found {totalResults} results ({searchTime.toFixed(2)} seconds)
            </div>

            <SearchResults results={currentResults} />

            <Pagination
              resultsPerPage={resultsPerPage}
              totalResults={totalResults}
              currentPage={currentPage}
              paginate={setCurrentPage}
            />
          </>
        )}

        {!loading && results.length === 0 && searchTime > 0 && (
          <div className="no-results">No results found</div>
        )}
      </main>

      <footer className="app-footer">
        <p>Enjoy Searching | The Best for ever</p>
      </footer>
    </div>
  );
}

export default App;
