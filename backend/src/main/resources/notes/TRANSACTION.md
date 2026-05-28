USER
- Client can send TransactionRequest to the ADMIN: createTransaction()
+ TransactionRequest contains the amount of money they want to deposit/withdraw
+ The type of transaction: DEPOSIT and WITHDRAWAL (check TransactionType)
+ The service will create a new entity with user info, and a default status is PENDING
+ And then, save to DB
- Client can also see their transaction requests (This is user's history): getUserTransaction()
+ TransactionResponse contains basic info, amount of money they send, type of the transaction, status to see that if ADMIN verify for them, the date they send
+ And the controller will return a page of TransactionResponse order by createdAt
- Client can also delete their transaction too: deleteTransaction()
+ Client can delete their pending transaction
+ I forget to block that

ADMIN
- Admin will see the ClientRequest to check that who is the sender, how many they want to deposit/withdraw: getAllTransactionRequest()
- Admin can choose to verify that transaction (deposit()/withdraw()) or they can cancel that (cancel())
- If they accept then the status become SUCCESS, if they cancel then the status become FAILED
- They can only cancel the PENDING transaction