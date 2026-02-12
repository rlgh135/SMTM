import { Routes, Route } from 'react-router-dom';
import { StockAnalysisPage } from '@/pages/StockAnalysis';

function App() {
  return (
    <Routes>
      <Route path="/" element={<StockAnalysisPage />} />
    </Routes>
  );
}

export default App;
