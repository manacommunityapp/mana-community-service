function fn() {
  var env = karate.env || 'dev';

  var config = {
    baseUrl:         'http://localhost:8080/api',
    communityId:     1,
    inviteCode:      'TESTINVITE',
    adminIdentifier: 'admin@manacommunity.com',
    adminPassword:   'Admin@123',
    dbUrl:           'jdbc:postgresql://localhost:5432/manacommunity',
    dbUser:          'postgres',
    dbPassword:      'postgres'
  };

  if (env === 'staging') {
    config.baseUrl    = 'https://staging.manacommunity.com/api';
    config.dbUrl      = 'jdbc:postgresql://staging-db:5432/manacommunity';
    config.inviteCode = karate.properties['invite.code'] || config.inviteCode;
  }

  if (env === 'ci') {
    // CI picks up the seed-generated invite code via system property
    config.inviteCode = karate.properties['invite.code'] || 'TESTINVITE';
  }

  // Shared utility: phone pattern for bulk user creation
  config.testUserPhone = function(i) {
    return '90000' + ('0000' + i).slice(-5);   // 9000000001 … 9000000050
  };

  config.testUserAadhar = function(i) {
    return '10000000' + ('0000' + i).slice(-4); // 12-digit unique aadhar
  };

  return config;
}
