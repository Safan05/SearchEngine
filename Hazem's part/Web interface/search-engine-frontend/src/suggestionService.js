// In a real implementation, this would fetch from your Java backend
// For now, we'll use localStorage to store and retrieve suggestions

const STORAGE_KEY = "searchQueryHistory";

export function getSuggestions(prefix) {
  const history = getQueryHistory();

  return history
    .filter((item) => item.query.toLowerCase().startsWith(prefix.toLowerCase()))
    .sort((a, b) => b.count - a.count)
    .slice(0, 5)
    .map((item) => item.query);
}

export function saveQuery(query) {
  if (!query.trim()) return;

  const history = getQueryHistory();
  const existing = history.find(
    (item) => item.query.toLowerCase() === query.toLowerCase()
  );

  if (existing) {
    existing.count += 1;
  } else {
    history.push({ query, count: 1 });
  }

  localStorage.setItem(STORAGE_KEY, JSON.stringify(history));
}

function getQueryHistory() {
  const stored = localStorage.getItem(STORAGE_KEY);
  return stored ? JSON.parse(stored) : [];
}
