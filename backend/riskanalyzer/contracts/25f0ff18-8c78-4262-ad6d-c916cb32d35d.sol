pragma solidity ^0.5.17;

contract SemiSuspiciousVault {

    mapping(address => uint256) public balances;

    function deposit() public payable {
        balances[msg.sender] += msg.value;
    }

    function withdraw(uint256 amount) public {

        require(balances[msg.sender] >= amount, "Insufficient balance");

        // Suspicious pattern: external call before state update
        (bool success, ) = msg.sender.call.value(amount)("");
        require(success, "Transfer failed");

        balances[msg.sender] -= amount;
    }

    function getBalance() public view returns (uint256) {
        return balances[msg.sender];
    }
}