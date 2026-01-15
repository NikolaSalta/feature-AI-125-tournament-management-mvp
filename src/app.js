const express = require('express');
const bodyParser = require('body-parser');
const tournamentRoutes = require('./routes/tournaments');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(bodyParser.json());
app.use('/api/tournaments', tournamentRoutes);

if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
  });
}

module.exports = app;
