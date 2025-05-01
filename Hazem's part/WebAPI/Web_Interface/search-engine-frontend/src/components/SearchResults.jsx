import React from "react";

function SearchResults({ results }) {

  console.log(results);
  return (
    <div className="search-results">
      {results.map((result) => (
        <div key={result.id} className="result-item">
          <h3>
            <a href={result.url} target="_blank" rel="noopener noreferrer">
              {result.title}
            </a>
          </h3>
          <div className="result-url">
            <a href={result.url} target="_blank" rel="noopener noreferrer">
              {result.url}
            </a>
          </div>
          <p
            className="result-snippet"
            dangerouslySetInnerHTML={{ __html: result.snippet }}
          />
        </div>
      ))}
    </div>
  );
}

export default SearchResults;



