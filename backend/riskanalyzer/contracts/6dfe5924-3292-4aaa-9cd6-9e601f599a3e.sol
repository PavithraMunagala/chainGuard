pragma solidity ^0.5.0;

contract VulnerableTest {

    mapping(address => uint256) public balances;

    address public owner;

    constructor() public {
        owner = msg.sender;
    }

    // Deposit ether
    function deposit() public payable {
        balances[msg.sender] += msg.value;
    }

    // 🚨 REENTRANCY VULNERABILITY
    function withdraw(uint256 amount) public {
        require(balances[msg.sender] >= amount, "Insufficient balance");

        // External call BEFORE state update
        (bool success, ) = msg.sender.call.value(amount)("");
        require(success, "Transfer failed");

        // State update AFTER external call
        balances[msg.sender] -= amount;
    }

    // 🚨 DELEGATECALL RISK
    function executeDelegate(address _target, bytes memory _data) public {
        require(msg.sender == owner, "Not owner");

        (bool success, ) = _target.delegatecall(_data);
        require(success, "Delegatecall failed");
    }

    // 🚨 Unrestricted external low-level call
    function externalCall(address payable _to) public payable {
        _to.call.value(msg.value)("");
    }

    // 🚨 Selfdestruct risk
    function destroy() public {
        require(msg.sender == owner, "Not owner");
        selfdestruct(msg.sender);
    }
}