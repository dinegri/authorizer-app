# authorizer-app

# L4. Questão aberta

Faria o uso de filas de processamento. Falando mais especificamente de Kafka, é possível utilizar chave de partição, dessa forma poderia existir diversas partições
mas com a chave de partição usando o identificador da conta do usuário iriam para a mesma partição e funcionaria como uma fila na qual evitaria o processamento concorrente. O processamento ficaria isolado em um consumidor pertencente a um grupo consumidor e receberia o 'assign' da partição
